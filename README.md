# Work Timer

Aplicativo Android para registrar horas trabalhadas por check-in/check-out manual ou por uma geofence opcional.

## Regras implementadas

- Check-in e check-out manuais podem ser usados quantas vezes forem necessários.
- Com geolocalização ativada, entrar na área inicia o timer e sair pausa o timer.
- Um check-out manual dentro da área prevalece sobre a geofence. O timer só volta a iniciar automaticamente depois que o usuário sair e entrar novamente.
- A geolocalização é opcional e vem desativada.
- Sessões que atravessam a meia-noite são divididas. O novo dia começa em zero e continua contando se a sessão permanecia ativa.
- A meta diária é configurável entre 15 minutos e 24 horas.
- Ao atingir a meta, o Android emite uma notificação sonora uma vez naquele dia.
- Geofence e alarmes são restaurados após reinicialização ou atualização do aplicativo.

## Tecnologia

- Interface web empacotada com Capacitor 8.
- Integração Android nativa em Java.
- Google Play Services Location para geofencing.
- `AlarmManager`, `BroadcastReceiver` e notificações Android para meta e troca do dia.
- Dados locais em `SharedPreferences`, separados pela data local do aparelho.

## Preparar o projeto

Requisitos: Node.js, Android Studio, Android SDK e JDK compatíveis com o Capacitor 8.

```bash
npm install
npm run android:sync
npm run android:open
```

O diretório `android/` já está versionado. Não execute `npm run android:add` novamente quando ele já existir.

## Permissões no celular

1. Abra **Configurações** no aplicativo.
2. Defina a meta diária.
3. Ative a geolocalização somente se desejar o controle automático.
4. Toque em **Configurar permissões** e permita localização precisa e notificações.
5. Toque novamente no botão e, nas configurações do Android, escolha localização **Permitir o tempo todo**.
6. Use **Usar localização atual** ou informe as coordenadas e o raio manualmente.

O Android pode atrasar transições de geofence por economia de bateria. Um raio inicial entre 100 e 200 metros é recomendado.

## Gerar APK de teste

No Android Studio, use **Build > Build APK(s)**. Pela linha de comando:

```bash
cd android
./gradlew assembleDebug
```

O APK será criado em `android/app/build/outputs/apk/debug/`.

## Publicação

Antes de publicar no Google Play, substitua os ícones e splash screens padrão, configure assinatura de release e prepare a justificativa de localização em segundo plano. A Play Store permite essa permissão somente quando ela é essencial e claramente explicada ao usuário.
