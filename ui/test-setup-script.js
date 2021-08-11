global.beforeEach(() => {
    const appRoot = document.createElement('div');
    appRoot.setAttribute('id', 'root');
    document.body.appendChild(appRoot);

    const popupRoot = document.createElement('div');
    popupRoot.setAttribute('id', 'popup-root');
    document.body.appendChild(popupRoot);
});

global.afterEach(() => {
    document.body.removeChild(document.getElementById('root'));
    document.body.removeChild(document.getElementById('popup-root'));
});
