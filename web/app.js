const native = window.Capacitor?.Plugins?.WorkTimer;
const $ = id => document.getElementById(id);
let state = null;
let settingsDirty = false;

const fmt = ms => {const s=Math.max(0,Math.floor(ms/1000)),h=Math.floor(s/3600),m=Math.floor(s%3600/60);return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s%60).padStart(2,'0')}`};
const clock = ms => new Date(ms).toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'});
const toast = msg => { $('toast').textContent=msg;$('toast').classList.add('show');setTimeout(()=>$('toast').classList.remove('show'),2200) };

async function call(method, options={}) {
  if (!native) throw new Error('Open this project as an Android app');
  return native[method](options);
}

async function refresh() {
  try { state = await call('getState'); render(); } catch (e) { $('permissionState').textContent=e.message; }
}

function render() {
  if (!state) return;
  const now=Date.now(), active=state.activeStart!=null;
  const running=active ? now-state.activeStart : 0;
  const total=state.completedMs+running, goal=state.goalMinutes*60000;
  const remaining=Math.max(0,goal-total), pct=goal?Math.min(100,total/goal*100):100;
  $('statusDot').className=active?'active':'';$('statusText').textContent=active?'WORKING':'IDLE';
  $('elapsed').className='elapsed'+(active?' active':'');$('elapsed').textContent=fmt(running);
  $('since').textContent=active?`Since ${clock(state.activeStart)} · ${state.activeSource==='geofence'?'location':'manual'}`:'— timer stopped —';
  $('checkIn').disabled=active;$('checkOut').disabled=!active;
  $('total').textContent=fmt(total);$('remaining').textContent=fmt(remaining);$('percent').textContent=`${Math.floor(pct)}%`;
  $('bar').style.width=`${pct}%`;$('bar').className=pct>=100?'done':'';
  $('eta').textContent=active&&remaining>0?clock(now+remaining):(remaining===0?'✓':'—:—');
  $('goalLabel').textContent=`DAILY GOAL · ${(state.goalMinutes/60).toLocaleString('en-US')}H`;
  $('sessionCount').textContent=state.sessions.length+(active?1:0);
  const rows=state.sessions.map(s=>`<div class="session"><span>${clock(s.start)}</span><span>${clock(s.end)}</span><span>${fmt(s.end-s.start)}</span></div>`);
  if(active)rows.push(`<div class="session"><span>${clock(state.activeStart)}</span><span>ACTIVE</span><span>${fmt(running)}</span></div>`);
  $('sessions').innerHTML=rows.join('')||'<p class="empty">NO SESSIONS</p>';
  if (!settingsDirty) {
    $('goalHours').value=state.goalMinutes/60;$('geoEnabled').checked=state.geoEnabled;
    $('geoFields').hidden=!state.geoEnabled;$('latitude').value=state.latitude||'';$('longitude').value=state.longitude||'';$('radius').value=state.radiusMeters||150;
  }
  $('permissionState').textContent=state.geoEnabled?(state.fineLocationGranted?(state.backgroundLocationGranted?'Precise and background location are allowed.':'Precise location is allowed. “Allow all the time” is still required.'):'Precise location permission is required.'):'';
}

$('checkIn').onclick=async()=>{await call('startManual');await refresh();toast('CHECKED IN')};
$('checkOut').onclick=async()=>{await call('stopManual');await refresh();toast(state?.insideGeofence?'PAUSED · LOCATION DISABLED UNTIL EXIT':'CHECKED OUT')};
$('toggleSettings').onclick=()=>{const h=$('settingsBody').hidden;$('settingsBody').hidden=!h;$('toggleSettings').textContent=h?'CLOSE':'OPEN'};
$('geoEnabled').onchange=async()=>{settingsDirty=true;$('geoFields').hidden=!$('geoEnabled').checked;if($('geoEnabled').checked){try{const result=await call('requestPermissions');if(result?.next)toast(result.next)}catch(e){toast(e.message)}}};
$('goalHours').oninput=$('latitude').oninput=$('longitude').oninput=$('radius').oninput=()=>{settingsDirty=true};
$('useLocation').onclick=async()=>{try{const p=await call('getCurrentLocation');$('latitude').value=p.latitude;$('longitude').value=p.longitude;toast('LOCATION CAPTURED')}catch(e){toast(e.message)}};
$('permissions').onclick=async()=>{try{const result=await call('requestPermissions');if(result?.next)toast(result.next);await refresh()}catch(e){toast(e.message)}};
$('saveSettings').onclick=async()=>{try{await call('configure',{goalMinutes:Math.round(Number($('goalHours').value)*60),geoEnabled:$('geoEnabled').checked,latitude:Number($('latitude').value),longitude:Number($('longitude').value),radiusMeters:Number($('radius').value)});settingsDirty=false;await refresh();toast('SETTINGS SAVED')}catch(e){toast(e.message)}};
$('resetDay').onclick=async()=>{if(confirm('Delete all sessions for today?')){await call('resetDay');await refresh();toast('DAY RESET')}};

setInterval(()=>{const d=new Date();$('date').innerHTML=d.toLocaleDateString('en-US', {weekday:'short',day:'2-digit',month:'short',year:'numeric'}).toUpperCase()+`<br>${d.toLocaleTimeString('en-US')}`;if(state)render()},1000);
refresh();
