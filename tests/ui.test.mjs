import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import { readFile } from 'node:fs/promises';

class Element {
  constructor() { this.hidden=false; this.checked=false; this.disabled=false; this.value=''; this.textContent=''; this.innerHTML=''; this.className=''; this.style={}; this.classList={add(){},remove(){}}; }
}

const elementIds=['toast','permissionState','date','statusDot','statusText','elapsed','since','geofenceStatus','geofenceStatusText','checkIn','checkOut','total','remaining','percent','bar','eta','goalLabel','sessionCount','sessions','goalHours','geoEnabled','geoFields','latitude','longitude','radius','toggleSettings','settingsBody','useLocation','permissions','saveSettings','resetDay'];

async function loadApp(stateOverrides={}) {
  const elements=Object.fromEntries(elementIds.map(id=>[id,new Element()]));
  elements.settingsBody.hidden=true; elements.geoFields.hidden=true;
  let interval;
  let permissionCalls=0;
  const currentState={activeStart:null,completedMs:0,goalMinutes:480,sessions:[],geoEnabled:false,latitude:0,longitude:0,radiusMeters:150,insideGeofence:false,fineLocationGranted:false,backgroundLocationGranted:false,geofenceMonitoringActive:false,geofenceStateKnown:false,...stateOverrides};
  const native={
    async getState(){return currentState},
    async requestPermissions(){permissionCalls++;return {next:'Select precise location.'}},
    async getCurrentLocation(){return {latitude:47.17,longitude:9.47}},
    async configure(){},async startManual(){},async stopManual(){},async resetDay(){}
  };
  const document={getElementById:id=>elements[id],addEventListener(){},hidden:false};
  const context={window:{Capacitor:{Plugins:{WorkTimer:native}}},document,setInterval:fn=>{interval=fn},setTimeout:()=>{},confirm:()=>true,Date,Number,Math,console};
  vm.runInNewContext(await readFile(new URL('../web/app.js',import.meta.url),'utf8'),context);
  await new Promise(resolve=>setImmediate(resolve));
  return {elements,currentState,runInterval:()=>interval(),permissionCalls:()=>permissionCalls};
}

test('geofence form stays open and current-location button fills coordinates', async () => {
  const {elements,runInterval,permissionCalls}=await loadApp();

  elements.geoEnabled.checked=true;
  await elements.geoEnabled.onchange();
  assert.equal(elements.geoFields.hidden,false);
  assert.equal(permissionCalls(),1);

  runInterval();
  await new Promise(resolve=>setImmediate(resolve));
  assert.equal(elements.geoFields.hidden,false,'periodic rendering must not discard unsaved settings');

  await elements.useLocation.onclick();
  assert.equal(elements.latitude.value,47.17);
  assert.equal(elements.longitude.value,9.47);
});

test('location indicator distinguishes unknown, inside and outside states', async () => {
  const {elements,currentState,runInterval}=await loadApp({geoEnabled:true,fineLocationGranted:true,backgroundLocationGranted:true,geofenceMonitoringActive:true});

  assert.equal(elements.geofenceStatusText.textContent,'DETERMINING WORK AREA');
  assert.equal(elements.geofenceStatus.className,'geofence-status warning');

  Object.assign(currentState,{geofenceStateKnown:true,insideGeofence:true});
  runInterval();
  assert.equal(elements.geofenceStatusText.textContent,'INSIDE WORK AREA');
  assert.equal(elements.geofenceStatus.className,'geofence-status inside');

  currentState.insideGeofence=false;
  runInterval();
  assert.equal(elements.geofenceStatusText.textContent,'OUTSIDE WORK AREA');
  assert.equal(elements.geofenceStatus.className,'geofence-status outside');
});
