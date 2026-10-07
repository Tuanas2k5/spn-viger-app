/* Close mobile nav on link click */
(function () {
    var toggle = document.getElementById('nav-open');
    var links = document.querySelectorAll('.menu a, .navpanel .cta-pill');
    for (var i = 0; i < links.length; i++) {
        links[i].addEventListener('click', function () {
            if (toggle.checked) toggle.checked = false;
        });
    }

    /* Close on Escape key */
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && toggle.checked) {
            toggle.checked = false;
        }
    });
})();
