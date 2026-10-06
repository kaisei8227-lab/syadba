(function () {
    const storedUser = localStorage.getItem('user');

    // 未ログインの場合は強制的に login.html へ飛ばす
    if (!storedUser && !window.location.pathname.endsWith('/login.html')) {
        window.location.href = '/login.html';
    }
})();