import { Client } from '@stomp/stompjs';

let _client: Client | null = null;
let _ready = false;
const _onReadyCallbacks: Array<() => void> = [];

export function isWsReady(): boolean {
  return _ready;
}

export function onWsReady(callback: () => void): void {
  if (_ready) {
    callback();
  } else {
    _onReadyCallbacks.push(callback);
  }
}

export function getWsClient(): Client {
  if (!_client) {
    _client = new Client({
      brokerURL: 'ws://' + window.location.host + '/ws',
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        _ready = true;
        _onReadyCallbacks.forEach((cb) => cb());
        _onReadyCallbacks.length = 0;
      },
      onDisconnect: () => {
        _ready = false;
      },
      onWebSocketClose: () => {
        _ready = false;
      },
    });
  }
  return _client;
}

export function connectWs(token: string): void {
  const client = getWsClient();
  client.connectHeaders = {
    Authorization: `Bearer ${token}`,
  };
  if (!client.active) {
    client.activate();
  }
}

export function disconnectWs(): void {
  if (_client?.active) {
    _client.deactivate();
  }
  _ready = false;
}
