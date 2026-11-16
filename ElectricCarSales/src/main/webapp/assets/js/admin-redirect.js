(() => {
    const hash = window.location.hash;
    const page = hash === "#ai" ? "../customer/ai-assistant.jsp" : "../admin.jsp";
    const destination = new URL(page, window.location.href);
    destination.search = window.location.search;
    if (hash !== "#ai") destination.hash = hash;
    window.location.replace(destination);
})();
