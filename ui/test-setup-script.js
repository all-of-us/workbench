global.beforeEach(() => {
    const popupRoot = document.createElement('div');
    popupRoot.setAttribute('id', 'popup-root');
    document.body.appendChild(popupRoot);
});

global.afterEach(() => {
    document.body.removeChild(document.getElementById('popup-root'));
});
