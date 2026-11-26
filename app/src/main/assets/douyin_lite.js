(function () {
  if (window.__douyinLiteBridgeLoaded) return;
  window.__douyinLiteBridgeLoaded = true;

  function forwardIfFeed(text, replace) {
    if (!text || text.indexOf('"aweme_list"') === -1) return;
    if (window.DouyinLiteBridge && DouyinLiteBridge.onFeedJson) {
      DouyinLiteBridge.onFeedJson(text, !!replace);
    }
  }

  // Hook fetch
  var origFetch = window.fetch;
  if (origFetch) {
    window.fetch = function () {
      var args = arguments;
      var url = typeof args[0] === 'string' ? args[0] : (args[0] && args[0].url) || '';
      return origFetch.apply(this, args).then(function (resp) {
        try {
          if (url.indexOf('/aweme/v1/web/') !== -1) {
            resp.clone().text().then(function (txt) {
              forwardIfFeed(txt, url.indexOf('/search/') !== -1);
            });
          }
        } catch (e) {}
        return resp;
      });
    };
  }

  // Hook XMLHttpRequest
  var origOpen = XMLHttpRequest.prototype.open;
  var origSend = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function (m, url) {
    this.__dylUrl = url || '';
    return origOpen.apply(this, arguments);
  };
  XMLHttpRequest.prototype.send = function () {
    this.addEventListener('load', function () {
      try {
        var u = this.__dylUrl || '';
        if (u.indexOf('/aweme/v1/web/') !== -1 && typeof this.responseText === 'string') {
          forwardIfFeed(this.responseText, u.indexOf('/search/') !== -1);
        }
      } catch (e) {}
    });
    return origSend.apply(this, arguments);
  };

  window.__dylLoadMore = function () {
    try {
      var nextBtn = document.querySelector('[class*="xgplayer-playswitch-next"], [data-e2e="video-switch-next-arrow"]');
      if (nextBtn) nextBtn.click();
      else window.scrollBy(0, window.innerHeight * 2);
    } catch (e) {}
  };

  window.__dylFetchComments = function (awemeId) {
    fetch('/aweme/v1/web/comment/list/?device_platform=webapp&aid=6383&aweme_id=' + encodeURIComponent(awemeId) + '&cursor=0&count=20', {
      credentials: 'include'
    }).then(function (r) { return r.text(); }).then(function (txt) {
      if (window.DouyinLiteBridge) DouyinLiteBridge.onCommentsJson(txt);
    }).catch(function () {
      if (window.DouyinLiteBridge) DouyinLiteBridge.onCommentsJson('{}');
    });
  };

  window.__dylDigg = function (awemeId, type) {
    fetch('/aweme/v1/web/commit/item/digg/?device_platform=webapp&aid=6383&aweme_id=' + encodeURIComponent(awemeId) + '&type=' + type, {
      method: 'POST',
      credentials: 'include'
    }).catch(function () {});
  };

  window.__dylSearch = function (keyword) {
    fetch('/aweme/v1/web/search/item/?device_platform=webapp&aid=6383&keyword=' + encodeURIComponent(keyword) + '&offset=0&count=15', {
      credentials: 'include'
    }).then(function (r) { return r.text(); }).then(function (txt) {
      forwardIfFeed(txt, true);
    }).catch(function () {});
  };
})();
