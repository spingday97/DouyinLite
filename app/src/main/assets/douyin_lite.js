(function () {
  if (window.__douyinLiteLoaded) return;
  window.__douyinLiteLoaded = true;

  var S_KEY = 'douyin_lite_cfg_v1';
  var cfg = {
    blockAppBanner: true,
    cleanScreen: false,
    autoNext: false,
    speed: 1.0,
    x: null,
    y: 160
  };
  try {
    var saved = JSON.parse(localStorage.getItem(S_KEY) || '{}');
    for (var k in saved) cfg[k] = saved[k];
  } catch (e) {}

  function saveCfg() {
    try { localStorage.setItem(S_KEY, JSON.stringify(cfg)); } catch (e) {}
  }

  var styleEl = document.createElement('style');
  styleEl.id = 'dyl_style';
  document.documentElement.appendChild(styleEl);

  function applyStyles() {
    var css = '';
    if (cfg.blockAppBanner) {
      css += '[class*="download-btn"],[class*="open-app"],[class*="call-app"],[class*="bottom-guide"],[class*="guide-banner"],[class*="launch-app"]{display:none!important;}';
    }
    if (cfg.cleanScreen) {
      css += '[class*="video-info-detail"],[class*="right-action"],[class*="side-bar"],[class*="author-card"],[class*="title-container"],[class*="music-info"]{opacity:0!important;pointer-events:none!important;}';
    }
    styleEl.textContent = css;
  }

  function activeVideo() {
    var vids = document.querySelectorAll('video');
    var best = null;
    for (var i = 0; i < vids.length; i++) {
      var v = vids[i];
      var r = v.getBoundingClientRect();
      if (r.width > 50 && r.height > 50 && r.bottom > 0 && r.top < window.innerHeight) {
        best = v;
        if (!v.paused) return v;
      }
    }
    return best || vids[0] || null;
  }

  function applyPlayback() {
    var v = activeVideo();
    if (!v) return;
    if (v.playbackRate !== cfg.speed) v.playbackRate = cfg.speed;
    if (!v.__dylBound) {
      v.__dylBound = true;
      v.addEventListener('ended', function () {
        if (!cfg.autoNext) return;
        var nextBtn = document.querySelector('[class*="xgplayer-playswitch-next"], [data-e2e="video-switch-next-arrow"]');
        if (nextBtn) nextBtn.click();
        else window.scrollBy({ top: window.innerHeight, behavior: 'smooth' });
      });
    }
  }

  function getMediaUrl() {
    var v = activeVideo();
    if (!v) return '';
    if (v.currentSrc && v.currentSrc.indexOf('http') === 0) return v.currentSrc;
    if (v.src && v.src.indexOf('http') === 0) return v.src;
    var s = v.querySelector('source');
    return s && s.src && s.src.indexOf('http') === 0 ? s.src : '';
  }

  function toast(msg) {
    if (window.DouyinLiteBridge && DouyinLiteBridge.toast) {
      DouyinLiteBridge.toast(msg);
    }
  }

  // Build floating pill UI
  var root = document.createElement('div');
  root.id = 'dyl_root';
  root.style.cssText = 'position:fixed;z-index:2147483647;font-family:-apple-system,BlinkMacSystemFont,sans-serif;user-select:none;touch-action:none;';
  var initX = cfg.x !== null ? cfg.x : (window.innerWidth - 50);
  root.style.left = Math.max(8, Math.min(window.innerWidth - 46, initX)) + 'px';
  root.style.top = Math.max(40, Math.min(window.innerHeight - 60, cfg.y)) + 'px';

  var pill = document.createElement('div');
  pill.style.cssText = 'width:38px;height:38px;border-radius:19px;background:rgba(20,22,28,0.72);border:1px solid rgba(255,255,255,0.22);color:#fff;display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:700;box-shadow:0 4px 12px rgba(0,0,0,0.35);cursor:pointer;';
  pill.textContent = '轻';

  var panel = document.createElement('div');
  panel.style.cssText = 'display:none;width:210px;background:rgba(22,24,30,0.92);border:1px solid rgba(255,255,255,0.16);border-radius:16px;padding:12px;color:#fff;font-size:12px;box-shadow:0 8px 24px rgba(0,0,0,0.45);';

  function makeBtn(label, onClick) {
    var b = document.createElement('div');
    b.style.cssText = 'padding:7px 10px;margin-top:6px;border-radius:10px;background:rgba(255,255,255,0.10);text-align:center;font-weight:500;cursor:pointer;';
    b.textContent = label;
    b.onclick = onClick;
    return b;
  }

  var header = document.createElement('div');
  header.style.cssText = 'display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;font-weight:700;font-size:13px;';
  header.innerHTML = '<span>DouyinLite</span>';
  var closeBtn = document.createElement('span');
  closeBtn.textContent = '✕';
  closeBtn.style.cssText = 'padding:2px 6px;opacity:0.75;cursor:pointer;';
  closeBtn.onclick = function () { panel.style.display = 'none'; pill.style.display = 'flex'; };
  header.appendChild(closeBtn);
  panel.appendChild(header);

  var btnClean = makeBtn('', function () {
    cfg.cleanScreen = !cfg.cleanScreen;
    saveCfg(); applyStyles(); refreshLabels();
  });
  var btnAutoNext = makeBtn('', function () {
    cfg.autoNext = !cfg.autoNext;
    saveCfg(); refreshLabels();
  });
  var btnSpeed = makeBtn('', function () {
    var speeds = [1.0, 1.25, 1.5, 2.0];
    var idx = (speeds.indexOf(cfg.speed) + 1) % speeds.length;
    cfg.speed = speeds[idx];
    saveCfg(); applyPlayback(); refreshLabels();
  });
  var btnBanner = makeBtn('', function () {
    cfg.blockAppBanner = !cfg.blockAppBanner;
    saveCfg(); applyStyles(); refreshLabels();
  });
  var btnDownload = makeBtn('⬇ 保存当前视频', function () {
    var url = getMediaUrl();
    if (!url) { toast('未检测到可下载视频流'); return; }
    if (window.DouyinLiteBridge) {
      DouyinLiteBridge.downloadMedia(url, 'douyin_' + Date.now() + '.mp4', 'video/mp4');
    }
  });
  var btnCopy = makeBtn('🔗 复制视频直链', function () {
    var url = getMediaUrl();
    if (!url) { toast('未检测到视频流'); return; }
    if (window.DouyinLiteBridge) DouyinLiteBridge.copyText(url);
  });
  var btnHome = makeBtn('🏠 返回推荐首页', function () {
    if (window.DouyinLiteBridge) DouyinLiteBridge.reloadHome();
    panel.style.display = 'none'; pill.style.display = 'flex';
  });

  [btnClean, btnAutoNext, btnSpeed, btnBanner, btnDownload, btnCopy, btnHome].forEach(function (el) {
    panel.appendChild(el);
  });

  function refreshLabels() {
    btnClean.textContent = '清屏模式: ' + (cfg.cleanScreen ? '开' : '关');
    btnAutoNext.textContent = '自动连播: ' + (cfg.autoNext ? '开' : '关');
    btnSpeed.textContent = '播放倍速: ' + cfg.speed + 'x';
    btnBanner.textContent = '屏蔽引流横幅: ' + (cfg.blockAppBanner ? '开' : '关');
  }
  refreshLabels();

  // Touch drag on pill
  var dragging = false, moved = false, sx = 0, sy = 0, ox = 0, oy = 0;
  pill.addEventListener('touchstart', function (e) {
    var t = e.touches[0];
    dragging = true; moved = false;
    sx = t.clientX; sy = t.clientY;
    ox = parseFloat(root.style.left) || 0;
    oy = parseFloat(root.style.top) || 0;
  }, { passive: true });
  pill.addEventListener('touchmove', function (e) {
    if (!dragging) return;
    var t = e.touches[0];
    var dx = t.clientX - sx, dy = t.clientY - sy;
    if (Math.abs(dx) > 6 || Math.abs(dy) > 6) moved = true;
    if (moved) {
      var nx = Math.max(4, Math.min(window.innerWidth - 42, ox + dx));
      var ny = Math.max(24, Math.min(window.innerHeight - 48, oy + dy));
      root.style.left = nx + 'px';
      root.style.top = ny + 'px';
      cfg.x = nx; cfg.y = ny;
    }
  }, { passive: true });
  pill.addEventListener('touchend', function () {
    if (dragging && moved) saveCfg();
    else if (!moved) { pill.style.display = 'none'; panel.style.display = 'block'; }
    dragging = false;
  });
  pill.addEventListener('click', function () {
    if (!moved) { pill.style.display = 'none'; panel.style.display = 'block'; }
  });

  root.appendChild(pill);
  root.appendChild(panel);
  document.documentElement.appendChild(root);

  applyStyles();
  setInterval(applyPlayback, 800);
})();
