package com.example.cardgame.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.cardgame.model.Card;
import com.example.cardgame.service.CardService;

@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = "*") // フロントエンドからのCORSリクエストを許可
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * 全カード一覧を取得（カード図鑑・デッキ構築画面用）
     * GET /api/cards
     */
    @GetMapping
    public ResponseEntity<List<Card>> getAllCards() {
        List<Card> cards = cardService.getAllCards();
        return ResponseEntity.ok(cards);
    }

    /**
     * ID指定で特定カードを取得
     * GET /api/cards/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Card> getCardById(@PathVariable String id) {
        return cardService.getAllCards().stream()
                .filter(c -> String.valueOf(c.getId()).equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 所属（クラス）別にカード一覧を取得
     * GET /api/cards/class/貴族
     */
    @GetMapping("/class/{className}")
    public ResponseEntity<List<Card>> getCardsByClass(@PathVariable String className) {
        List<Card> cards = cardService.getCardsByClass(className);
        return ResponseEntity.ok(cards);
    }

    /**
     * 条件絞り込み検索（デッキビルダー用）
     * GET /api/cards/search?class=貴族&cost=3&rarity=エース
     */
    @GetMapping("/search")
    public ResponseEntity<List<Card>> searchCards(
            @RequestParam(required = false) String cardClass,
            @RequestParam(required = false) Integer cost,
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false) String name) {

        List<Card> filtered = cardService.getAllCards().stream()
                .filter(c -> cardClass == null || cardClass.isEmpty() || c.getCardClass().equalsIgnoreCase(cardClass))
                .filter(c -> cost == null || c.getCost() == cost)
                .filter(c -> rarity == null || rarity.isEmpty() || c.getRarity().equalsIgnoreCase(rarity))
                .filter(c -> name == null || name.isEmpty() || c.getName().contains(name))
                .toList();

        return ResponseEntity.ok(filtered);
    }
}