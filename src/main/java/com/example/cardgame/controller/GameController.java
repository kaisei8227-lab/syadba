package com.example.cardgame.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.example.cardgame.model.Card;
import com.example.cardgame.model.GameRoom;
import com.example.cardgame.model.Player;
import com.example.cardgame.service.CardService;

@Controller
public class GameController {

    @Autowired
    private CardService cardService;

    private GameRoom gameRoom = new GameRoom();

    /**
     * プレイヤーの入室・参加処理
     */
    @MessageMapping("/join")
    @SendTo("/topic/game")
    public GameRoom joinGame(Map<String, String> payload) {
        String playerId = payload.get("playerId");
        String selectedClass = payload.getOrDefault("cardClass", "貴族");

        // すでに勝敗が決まっている（前のゲームが終わっている）場合は部屋をリセット
        if (gameRoom.getWinnerPlayerId() != null) {
            gameRoom.reset();
        }

        // プレイヤー1として参加
        if (gameRoom.getPlayer1() == null) {
            Player p1 = new Player(playerId);
            p1.setDeck(cardService.generateRandomDeck(selectedClass));
            gameRoom.setPlayer1(p1);
        } 
        // プレイヤー2として参加（P1と異なるIDの場合）
        else if (gameRoom.getPlayer2() == null && !playerId.equals(gameRoom.getPlayer1().getId())) {
            Player p2 = new Player(playerId);
            p2.setDeck(cardService.generateRandomDeck(selectedClass));
            gameRoom.setPlayer2(p2);
            // 2人揃ったらゲーム開始
            gameRoom.startGame();
        }
        return gameRoom;
    }

    /**
     * アクション処理（カードのプレイ、攻撃、ターン終了など）
     */
    @MessageMapping("/action")
    @SendTo("/topic/game")
    public GameRoom handleAction(Map<String, Object> action) {
        String type = (String) action.get("type");
        String playerId = (String) action.get("playerId");

        // ゲーム未開始、または自分のターンでない場合は何もしない
        if (!gameRoom.isStarted() || !playerId.equals(gameRoom.getCurrentTurnPlayerId())) {
            return gameRoom;
        }

        // 自分と相手のプレイヤーオブジェクトを取得
        Player me = playerId.equals(gameRoom.getPlayer1().getId()) ? gameRoom.getPlayer1() : gameRoom.getPlayer2();
        Player enemy = playerId.equals(gameRoom.getPlayer1().getId()) ? gameRoom.getPlayer2() : gameRoom.getPlayer1();

        switch (type) {
            case "PLAY_CARD": {
                // 手札からカードを場に出す
                String cardId = String.valueOf(action.get("cardId"));
                Card cardToPlay = me.getHand().stream()
                        .filter(c -> String.valueOf(c.getId()).equals(cardId))
                        .findFirst()
                        .orElse(null);

                if (cardToPlay != null && me.getCurrentPp() >= cardToPlay.getCost() && me.getBoard().size() < 5) {
                    me.setCurrentPp(me.getCurrentPp() - cardToPlay.getCost());
                    me.getHand().remove(cardToPlay);
                    cardToPlay.setCanAttack(false); // 召喚酔い
                    me.getBoard().add(cardToPlay);
                }
                break;
            }

            case "ATTACK_PLAYER": {
                // 相手プレイヤーへのダイレクトアタック
                String attackerId = String.valueOf(action.get("attackerId"));
                Card attacker = me.getBoard().stream()
                        .filter(c -> String.valueOf(c.getId()).equals(attackerId))
                        .findFirst()
                        .orElse(null);

                if (attacker != null && attacker.isCanAttack()) {
                    enemy.setHp(enemy.getHp() - attacker.getAttack());
                    attacker.setCanAttack(false);
                }
                break;
            }

            case "ATTACK_CARD": {
                // フォロワー同士の戦闘
                String myAttackerId = String.valueOf(action.get("attackerId"));
                String targetCardId = String.valueOf(action.get("targetId"));

                Card myCard = me.getBoard().stream()
                        .filter(c -> String.valueOf(c.getId()).equals(myAttackerId))
                        .findFirst()
                        .orElse(null);

                Card targetCard = enemy.getBoard().stream()
                        .filter(c -> String.valueOf(c.getId()).equals(targetCardId))
                        .findFirst()
                        .orElse(null);

                if (myCard != null && targetCard != null && myCard.isCanAttack()) {
                    targetCard.setCurrentHp(targetCard.getCurrentHp() - myCard.getAttack());
                    myCard.setCurrentHp(myCard.getCurrentHp() - targetCard.getAttack());
                    myCard.setCanAttack(false);

                    if (targetCard.getCurrentHp() <= 0) {
                        enemy.getBoard().remove(targetCard);
                    }
                    if (myCard.getCurrentHp() <= 0) {
                        me.getBoard().remove(myCard);
                    }
                }
                break;
            }

            case "END_TURN": {
                // ターン終了
                gameRoom.endTurn();
                break;
            }
        }

        // 勝敗チェック
        gameRoom.checkWinCondition();

        return gameRoom;
    }

    /**
     * 手動で部屋をリセットしたい場合（ロビー戻り時など）のハンドラー
     */
    @MessageMapping("/reset")
    @SendTo("/topic/game")
    public GameRoom resetGame() {
        gameRoom.reset();
        return gameRoom;
    }
}