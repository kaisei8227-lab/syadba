package com.example.cardgame.model;

public class GameRoom {

    private Player player1;
    private Player player2;
    private String currentTurnPlayerId;
    private boolean started = false;
    private String winnerPlayerId; // 勝利したプレイヤーのID（決着前はnull）

    public GameRoom() {
    }

    /**
     * 部屋を初期状態にリセット（ゲーム終了後や再入室用）
     */
    public void reset() {
        this.player1 = null;
        this.player2 = null;
        this.currentTurnPlayerId = null;
        this.started = false;
        this.winnerPlayerId = null;
    }

    /**
     * ゲーム開始処理（2人揃った時に呼び出し）
     */
    public void startGame() {
        if (player1 == null || player2 == null) {
            return;
        }

        this.started = true;
        this.winnerPlayerId = null; // 勝者判定をリセット
        
        // 初期手札のドロー（例：3枚ずつ）
        for (int i = 0; i < 3; i++) {
            player1.drawCard();
            player2.drawCard();
        }

        // 先攻を Player 1 に設定してターン1を開始
        this.currentTurnPlayerId = player1.getId();
        startTurn(player1);
    }

    /**
     * ターン終了処理（プレイヤーの操作で呼び出し）
     */
    public void endTurn() {
        if (!started || winnerPlayerId != null) {
            return;
        }

        // ターンプレイヤーの交替
        if (currentTurnPlayerId.equals(player1.getId())) {
            currentTurnPlayerId = player2.getId();
            startTurn(player2);
        } else {
            currentTurnPlayerId = player1.getId();
            startTurn(player1);
        }
    }

    /**
     * ターン開始共通処理（PP増加・ドロー・召喚酔い解除）
     */
    private void startTurn(Player player) {
        // 1. 最大PPを +1（上限10）
        if (player.getMaxPp() < 10) {
            player.setMaxPp(player.getMaxPp() + 1);
        }
        
        // 2. 現在PPを最大値まで回復
        player.setCurrentPp(player.getMaxPp());

        // 3. 自分の盤面フォロワーの攻撃可能フラグを復元（召喚酔い解除）
        if (player.getBoard() != null) {
            for (Card card : player.getBoard()) {
                card.setCanAttack(true);
            }
        }

        // 4. デッキからカードを1枚ドロー
        player.drawCard();
    }

    /**
     * 勝敗判定（ゲーム中かつ両プレイヤーが存在する場合のみ実行）
     */
    public void checkWinCondition() {
        if (!started || player1 == null || player2 == null) {
            return;
        }

        if (player1.getHp() <= 0 && player2.getHp() <= 0) {
            this.winnerPlayerId = "DRAW";
        } else if (player1.getHp() <= 0) {
            this.winnerPlayerId = player2.getId();
        } else if (player2.getHp() <= 0) {
            this.winnerPlayerId = player1.getId();
        }
    }

    // --- ゲッター & セッター ---

    public Player getPlayer1() {
        return player1;
    }

    public void setPlayer1(Player player1) {
        this.player1 = player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

    public String getCurrentTurnPlayerId() {
        return currentTurnPlayerId;
    }

    public void setCurrentTurnPlayerId(String currentTurnPlayerId) {
        this.currentTurnPlayerId = currentTurnPlayerId;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public String getWinnerPlayerId() {
        return winnerPlayerId;
    }

    public void setWinnerPlayerId(String winnerPlayerId) {
        this.winnerPlayerId = winnerPlayerId;
    }
}