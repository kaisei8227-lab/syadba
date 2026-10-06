let stompClient = null;
let currentUser = null;
let selectedClass = '貴族';
let myPlayerId = '';

document.addEventListener('DOMContentLoaded', () => {
    // ログイン情報の確認
    const storedUser = localStorage.getItem('user');
    if (!storedUser) {
        // 未ログインの場合はログイン画面へ遷移
        window.location.href = '/login.html';
        return;
    }
    
    currentUser = JSON.parse(storedUser);
    myPlayerId = currentUser.username; // ユーザー名をプレイヤーIDとして使用

    // プレイヤー名の表示更新（UI要素がある場合）
    const nameDisplay = document.getElementById('player-name-display');
    if (nameDisplay) {
        nameDisplay.innerText = myPlayerId;
    }

    // WebSocket通信の接続開始
    connectWebSocket();
});

/**
 * WebSocket (STOMP) 接続設定
 */
function connectWebSocket() {
    const socket = new SockJS('/ws-game');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, (frame) => {
        console.log('Connected: ' + frame);

        // ゲーム状態の更新を受信
        stompClient.subscribe('/topic/game', (message) => {
            const gameRoom = JSON.parse(message.body);
            updateGameState(gameRoom);
        });
    }, (error) => {
        console.error('STOMP Error:', error);
    });
}

/**
 * 部屋に入室（対戦相手待ち / 対戦開始）
 */
function joinRoom() {
    if (!stompClient || !stompClient.connected) return;

    // クラス選択を取得（UIがある場合）
    const classSelect = document.getElementById('class-select');
    if (classSelect) {
        selectedClass = classSelect.value;
    }

    stompClient.send('/app/join', {}, JSON.stringify({
        playerId: myPlayerId,
        cardClass: selectedClass
    }));
}

/**
 * ゲーム状態（GameRoom）の画面描画更新
 */
function updateGameState(gameRoom) {
    if (!gameRoom) return;

    // 1. 勝敗が決まっている場合の処理
    if (gameRoom.winnerPlayerId) {
        showGameResult(gameRoom.winnerPlayerId);
        return;
    }

    // 2. 対戦画面の表示切り替え（ロビー画面を非表示、対戦盤面を表示）
    const lobbySection = document.getElementById('lobby-section');
    const gameSection = document.getElementById('game-section');
    const resultOverlay = document.getElementById('result-overlay');

    if (lobbySection) lobbySection.style.display = gameRoom.started ? 'none' : 'block';
    if (gameSection) gameSection.style.display = gameRoom.started ? 'block' : 'none';
    if (resultOverlay) resultOverlay.style.display = 'none';

    if (!gameRoom.started) return;

    // 自分のプレイヤーオブジェクトと相手のプレイヤーオブジェクトを特定
    const isP1 = (gameRoom.player1 && gameRoom.player1.id === myPlayerId);
    const me = isP1 ? gameRoom.player1 : gameRoom.player2;
    const enemy = isP1 ? gameRoom.player2 : gameRoom.player1;

    // ターン表示やHP/PPなどのUI更新
    renderPlayerStatus(me, enemy, gameRoom.currentTurnPlayerId);
    
    // 自分の手札・自分の盤面・相手の盤面の描画
    if (me) {
        renderHand(me.hand);
        renderBoard('my-board', me.board, true);
    }
    if (enemy) {
        renderBoard('enemy-board', enemy.board, false);
    }
}

/**
 * カードプレイ（手札から出す）
 */
function playCard(cardId) {
    stompClient.send('/app/action', {}, JSON.stringify({
        type: 'PLAY_CARD',
        playerId: myPlayerId,
        cardId: cardId
    }));
}

/**
 * 相手プレイヤーへダイレクトアタック
 */
function attackPlayer(attackerId) {
    stompClient.send('/app/action', {}, JSON.stringify({
        type: 'ATTACK_PLAYER',
        playerId: myPlayerId,
        attackerId: attackerId
    }));
}

/**
 * 相手のフォロワーへ攻撃
 */
function attackCard(attackerId, targetId) {
    stompClient.send('/app/action', {}, JSON.stringify({
        type: 'ATTACK_CARD',
        playerId: myPlayerId,
        attackerId: attackerId,
        targetId: targetId
    }));
}

/**
 * ターン終了
 */
function endTurn() {
    stompClient.send('/app/action', {}, JSON.stringify({
        type: 'END_TURN',
        playerId: myPlayerId
    }));
}

/**
 * 勝敗（DEFEAT / VICTORY）オーバーレイ表示
 */
function showGameResult(winnerId) {
    const resultOverlay = document.getElementById('result-overlay');
    const resultText = document.getElementById('result-text');

    if (!resultOverlay || !resultText) return;

    resultOverlay.style.display = 'flex';

    if (winnerId === 'DRAW') {
        resultText.innerText = 'DRAW';
        resultText.style.color = '#f1c40f';
    } else if (winnerId === myPlayerId) {
        resultText.innerText = 'VICTORY';
        resultText.style.color = '#2ecc71';
    } else {
        resultText.innerText = 'DEFEAT';
        resultText.style.color = '#e74c3c';
    }
}

/**
 * 「ロビーへ戻る」ボタンを押したときの処理（部屋のリセットと画面復帰）
 */
function backToLobby() {
    // サーバーに部屋のリセットリクエストを送信
    if (stompClient && stompClient.connected) {
        stompClient.send('/app/reset', {}, JSON.stringify({}));
    }

    // 画面の勝敗ダイアログを隠してページリロード
    const resultOverlay = document.getElementById('result-overlay');
    if (resultOverlay) {
        resultOverlay.style.display = 'none';
    }

    location.reload();
}

/* --- UI描画補助関数 --- */

function renderPlayerStatus(me, enemy, currentTurnPlayerId) {
    const turnIndicator = document.getElementById('turn-indicator');
    if (turnIndicator) {
        const isMyTurn = (currentTurnPlayerId === myPlayerId);
        turnIndicator.innerText = isMyTurn ? 'あなたのターン' : '相手のターン';
        turnIndicator.style.color = isMyTurn ? '#2ecc71' : '#e74c3c';
    }

    if (me) {
        document.getElementById('my-hp').innerText = `HP: ${me.hp}`;
        document.getElementById('my-pp').innerText = `PP: ${me.currentPp} / ${me.maxPp}`;
    }
    if (enemy) {
        document.getElementById('enemy-hp').innerText = `HP: ${enemy.hp}`;
        document.getElementById('enemy-pp').innerText = `PP: ${enemy.currentPp} / ${enemy.maxPp}`;
    }
}

function renderHand(hand) {
    const container = document.getElementById('my-hand');
    if (!container) return;
    container.innerHTML = '';

    hand.forEach(card => {
        const el = document.createElement('div');
        el.className = 'card';
        el.onclick = () => playCard(card.id);
        el.innerHTML = `
            <div class="card-cost">${card.cost}</div>
            <div class="card-name">${card.name}</div>
            <div class="card-stats">⚔️ ${card.attack} / ❤️ ${card.currentHp || card.defense}</div>
        `;
        container.appendChild(el);
    });
}

function renderBoard(elementId, board, isMyBoard) {
    const container = document.getElementById(elementId);
    if (!container) return;
    container.innerHTML = '';

    board.forEach(card => {
        const el = document.createElement('div');
        el.className = `card board-card ${card.canAttack ? 'can-attack' : ''}`;
        
        if (isMyBoard && card.canAttack) {
            el.onclick = () => attackPlayer(card.id); // デフォルトで相手リーダー攻撃（ターゲット選択UIに応じて調整可）
        }

        el.innerHTML = `
            <div class="card-name">${card.name}</div>
            <div class="card-stats">⚔️ ${card.attack} / ❤️ ${card.currentHp}</div>
        `;
        container.appendChild(el);
    });
}