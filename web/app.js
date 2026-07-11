const native = window.Capacitor?.Plugins?.WorkTimer;
const $ = id => document.getElementById(id);
let state = null;

const fmt = ms => {const s=Math.max(0,Math.floor(ms/1000)),h=Math.floor(s/3600),m=Math.floor(s%3600/60);return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s%60).padStart(2,'0')}`};
const clock = ms => new Date(ms).toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'});
const toast = msg => { $('toast').textContent=msg;$('toast').classList.add('show');setTimeout(()=>$('toast').classList.remove('show'),2200) };

async function call(method, options={}) {
  if (!native) throw new Error('Abra o projeto como aplicativo Android');
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
  $('statusDot').className=active?'active':'';$('statusText').textContent=active?'TRABALHANDO':'PARADO';
  $('elapsed').className='elapsed'+(active?' active':'');$('elapsed').textContent=fmt(running);
  $('since').textContent=active?`Desde ${clock(state.activeStart)} · ${state.activeSource==='geofence'?'localização':'manual'}`:'— timer parado —';
  $('checkIn').disabled=active;$('checkOut').disabled=!active;
  $('total').textContent=fmt(total);$('remaining').textContent=fmt(remaining);$('percent').textContent=`${Math.floor(pct)}%`;
  $('bar').style.width=`${pct}%`;$('bar').className=pct>=100?'done':'';
  $('eta').textContent=active&&remaining>0?clock(now+remaining):(remaining===0?'✓':'—:—');
  $('goalLabel').textContent=`META DIÁRIA · ${(state.goalMinutes/60).toLocaleString()}H`;
  $('sessionCount').textContent=state.sessions.length+(active?1:0);
  const rows=state.sessions.map(s=>`<div class="session"><span>${clock(s.start)}</span><span>${clock(s.end)}</span><span>${fmt(s.end-s.start)}</span></div>`);
  if(active)rows.push(`<div class="session"><span>${clock(state.activeStart)}</span><span>EM CURSO</span><span>${fmt(running)}</span></div>`);
  $('sessions').innerHTML=rows.join('')||'<p class="empty">NENHUMA SESSÃO</p>';
  $('goalHours').value=state.goalMinutes/60;$('geoEnabled').checked=state.geoEnabled;
  $('geoFields').hidden=!state.geoEnabled;$('latitude').value=state.latitude||'';$('longitude').value=state.longitude||'';$('radius').value=state.radiusMeters||150;
  $('permissionState').textContent=state.geoEnabled?(state.backgroundLocationGranted?'Localização em segundo plano autorizada.':'Falta permitir localização “o tempo todo”.'):'';
}

$('checkIn').onclick=async()=>{await call('startManual');await refresh();toast('CHECK IN REGISTRADO')};
$('checkOut').onclick=async()=>{await call('stopManual');await refresh();toast(state?.insideGeofence?'PAUSADO · LOCALIZAÇÃO BLOQUEADA ATÉ SAIR':'CHECK OUT REGISTRADO')};
$('toggleSettings').onclick=()=>{const h=$('settingsBody').hidden;$('settingsBody').hidden=!h;$('toggleSettings').textContent=h?'FECHAR':'ABRIR'};
$('geoEnabled').onchange=()=>{$('geoFields').hidden=!$('geoEnabled').checked};
$('useLocation').onclick=async()=>{try{const p=await call('getCurrentLocation');$('latitude').value=p.latitude;$('longitude').value=p.longitude;toast('LOCALIZAÇÃO CAPTURADA')}catch(e){toast(e.message)}};
$('permissions').onclick=async()=>{try{await call('requestPermissions');await refresh()}catch(e){toast(e.message)}};
$('saveSettings').onclick=async()=>{try{await call('configure',{goalMinutes:Math.round(Number($('goalHours').value)*60),geoEnabled:$('geoEnabled').checked,latitude:Number($('latitude').value),longitude:Number($('longitude').value),radiusMeters:Number($('radius').value)});await refresh();toast('CONFIGURAÇÕES SALVAS')}catch(e){toast(e.message)}};
$('resetDay').onclick=async()=>{if(confirm('Apagar todas as sessões de hoje?')){await call('resetDay');await refresh();toast('DIA RESETADO')}};

setInterval(()=>{const d=new Date();$('date').innerHTML=d.toLocaleDateString([], {weekday:'short',day:'2-digit',month:'short',year:'numeric'}).toUpperCase()+`<br>${d.toLocaleTimeString()}`;if(state)render()},1000);
refresh();
