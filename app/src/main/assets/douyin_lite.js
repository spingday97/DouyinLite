(function () {
  if (window.__douyinLiteBridgeLoaded) return;
  window.__douyinLiteBridgeLoaded = true;

  function isFeedUrl(url) {
    if (!url) return false;
    return (
      url.indexOf('/aweme/v1/web/tab/feed') !== -1 ||
      url.indexOf('/aweme/v1/web/familiar/feed') !== -1 ||
      url.indexOf('/aweme/v1/web/follow/feed') !== -1 ||
      url.indexOf('/aweme/v1/web/search/item') !== -1
    );
  }

  function forwardIfFeed(text, replace) {
    if (!text || text.indexOf('"aweme_list"') === -1) return;
    if (window.DouyinLiteBridge && DouyinLiteBridge.onFeedJson) {
      DouyinLiteBridge.onFeedJson(text, !!replace);
    }
  }

  function tryExtractSsr() {
    try {
      var el = document.getElementById('RENDER_DATA');
      if (el && el.textContent) {
        var decoded = decodeURIComponent(el.textContent);
        forwardIfFeed(decoded, false);
      }
    } catch (e) {}
  }
  window.__dylTrySsr = tryExtractSsr;
  if (document.readyState !== 'loading') {
    tryExtractSsr();
  } else {
    document.addEventListener('DOMContentLoaded', tryExtractSsr);
  }

  var origFetch = window.fetch;
  if (origFetch) {
    window.fetch = function () {
      var args = arguments;
      var url = typeof args[0] === 'string' ? args[0] : (args[0] && args[0].url) || '';
      return origFetch.apply(this, args).then(function (resp) {
        try {
          if (isFeedUrl(url)) {
            resp.clone().text().then(function (txt) {
              forwardIfFeed(txt, url.indexOf('/search/') !== -1);
            });
          }
        } catch (e) {}
        return resp;
      });
    };
  }

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
        if (isFeedUrl(u) && typeof this.responseText === 'string') {
          forwardIfFeed(this.responseText, u.indexOf('/search/') !== -1);
        }
      } catch (e) {}
    });
    return origSend.apply(this, arguments);
  };

  window.__dylFetchRecommend = function (replace) {
    fetch('/aweme/v1/web/tab/feed/?device_platform=webapp&aid=6383&channel=channel_pc_web&count=12&refresh_index=1', {
      credentials: 'include'
    }).then(function (r) { return r.text(); }).then(function (txt) {
      forwardIfFeed(txt, !!replace);
    }).catch(function () {});
  };

  window.__dylLoadMore = function () {
    try {
      var nextBtn = document.querySelector('[class*="xgplayer-playswitch-next"], [data-e2e="video-switch-next-arrow"]');
      if (nextBtn) nextBtn.click();
      else window.scrollBy(0, window.innerHeight * 2);
    } catch (e) {}
    if (window.__dylFetchRecommend) window.__dylFetchRecommend(false);
  };

  window.__dylSwitchTab = function (tab) {
    var url = '/aweme/v1/web/tab/feed/?device_platform=webapp&aid=6383&channel=channel_pc_web&count=12&refresh_index=1';
    if (tab === 'friend') {
      url = '/aweme/v1/web/familiar/feed/?device_platform=webapp&aid=6383&count=12';
    } else if (tab === 'follow') {
      url = '/aweme/v1/web/follow/feed/?device_platform=webapp&aid=6383&count=12';
    }
    fetch(url, {
      credentials: 'include'
    }).then(function (r) { return r.text(); }).then(function (txt) {
      forwardIfFeed(txt, true);
    }).catch(function () {});
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

  window.__dylCollect = function (awemeId, action) {
    fetch('/aweme/v1/web/aweme/collect/?device_platform=webapp&aid=6383&aweme_id=' + encodeURIComponent(awemeId) + '&action=' + action, {
      method: 'POST',
      credentials: 'include'
    }).catch(function () {});
  };

  window.__dylSearch = function (keyword, sortType) {
    fetch('/aweme/v1/web/search/item/?device_platform=webapp&aid=6383&keyword=' + encodeURIComponent(keyword) + '&offset=0&count=15&sort_type=' + (sortType || 0), {
      credentials: 'include'
    }).then(function (r) { return r.text(); }).then(function (txt) {
      forwardIfFeed(txt, true);
    }).catch(function () {});
  };

  window.__dylFetchUserPosts = function (secUid) {
    fetch('/aweme/v1/web/aweme/post/?device_platform=webapp&aid=6383&sec_user_id=' + encodeURIComponent(secUid) + '&max_cursor=0&count=18', {
      credentials: 'include'
    }).then(function (r) { return r.text(); }).then(function (txt) {
      if (window.DouyinLiteBridge && DouyinLiteBridge.onProfilePostsJson) {
        DouyinLiteBridge.onProfilePostsJson(txt);
      }
    }).catch(function () {});
  };

  window.__dylFetchMyList = function (kind) {
    var url = kind === 'collect'
      ? '/aweme/v1/web/aweme/listcollection/?device_platform=webapp&aid=6383&cursor=0&count=18'
      : '/aweme/v1/web/aweme/favorite/?device_platform=webapp&aid=6383&max_cursor=0&count=18';
    fetch(url, {
      credentials: 'include'
    }).then(function (r) { return r.text(); }).then(function (txt) {
      if (window.DouyinLiteBridge && DouyinLiteBridge.onProfilePostsJson) {
        DouyinLiteBridge.onProfilePostsJson(txt);
      }
    }).catch(function () {});
  };

  window.__dylFollow = function (userId, secUid, type) {
    fetch('/aweme/v1/web/commit/follow/user/?device_platform=webapp&aid=6383&user_id=' + encodeURIComponent(userId || '') + '&sec_user_id=' + encodeURIComponent(secUid || '') + '&type=' + type, {
      method: 'POST',
      credentials: 'include'
    }).catch(function () {});
  };
})(); 
