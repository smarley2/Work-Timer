import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import { readFile } from 'node:fs/promises';

class Element {
  constructor() { this.hidden=false; this.checked=false; this.disabled=false; this.value=''; this.style={}; this.classList={add(){},remove(){}}; }
}

test('geofence form stays open and current-location button fills coordinates', async () => {
  const ids=['toast','permissionState','date','statusDot','statusText','elapsed','since','checkIn','checkOut','total','remaining','percent','bar','eta','goalLabel','sessionCount','sessions','goalHours','geoEnabled','geoFields','latitude','longitude','radius','toggleSettings','settingsBody','useLocation','permissions','saveSettings','resetDay'];
  const elements=Object.fromEntries(ids.map(id=>[id,new Element()]));
  elements.settingsBody.hidden=true; elements.geoFields.hidden=true;
  let interval;
  let permissionCalls=0;
  const native={
    async getState(){return {activeStart:null,completedMs:0,goalMinutes:480,sessions:[],geoEnabled:false,latitude:0,longitude:0,radiusMeters:150,fineLocationGranted:false,backgroundLocationGranted:false}},
    async requestPermissions(){permissionCalls++;return {next:'Selecione localização precisa.'}},
    async getCurrentLocation(){return {latitude:47.17,longitude:9.47}},
    async configure(){},async startManual(){},async stopManual(){},async resetDay(){}
  };
  const context={window:{Capacitor:{Plugins:{WorkTimer:native}}},document:{getElementById:id=>elements[id]},setInterval:fn=>{interval=fn},setTimeout:()=>{},confirm:()=>true,Date,Number,Math,console};
  vm.runInNewContext(await readFile(new URL('../web/app.js',import.meta.url),'utf8'),context);
  await new Promise(resolve=>setImmediate(resolve));

  elements.geoEnabled.checked=true;
  await elements.geoEnabled.onchange();
  assert.equal(elements.geoFields.hidden,false);
  assert.equal(permissionCalls,1);

  interval();
  await new Promise(resolve=>setImmediate(resolve));
  assert.equal(elements.geoFields.hidden,false,'periodic rendering must not discard unsaved settings');

  await elements.useLocation.onclick();
  assert.equal(elements.latitude.value,47.17);
  assert.equal(elements.longitude.value,9.47);
});
