Версия для повторной подачи на модерацию RuStore.

Удалены все 9 разрешений, перечисленных модератором: QUERY_ALL_PACKAGES,
READ_CALL_LOG, READ_CONTACTS, ACCESS_BACKGROUND_LOCATION, ACCESS_FINE_LOCATION,
ACCESS_COARSE_LOCATION, READ_PHONE_STATE, MANAGE_ONGOING_CALLS,
BIND_NOTIFICATION_LISTENER_SERVICE. Удалена служба чтения сторонних уведомлений.

Bluetooth-поиск адаптирован для работы без геолокации. Разрешение
«Устройства поблизости» по-прежнему требуется. Код папок, USB-обновления,
плей-листов, массового выделения и сохранения прокрутки не изменён.

Пакет: `com.evcherarch.suuntoaquamusicfolders`.
Версия: `1.0.1-rustore`, код версии `6013009`. Android 12L/API 32 и новее.
Подписано тем же сертификатом, что и предыдущий APK с уникальным пакетом.

Проверены сборка, подпись, выравнивание APK и отсутствие перечисленных
разрешений в бинарном манифесте. Живая проверка Bluetooth, USB и звонков
на телефоне ещё не выполнена. Поэтому релиз помечен как предварительный.
Функции GPS и передачи уведомлений исходного Suunto недоступны.

Предыдущий релиз [v1.0.0](https://github.com/Evcher-arch/suunto-aqua-folder-picker/releases/tag/v1.0.0) сохранён.
Обновление можно установить поверх него. Возврат на старую версию с меньшим
кодом версии стандартной установкой Android не поддерживается.

[Подробности и проверка на телефоне](https://github.com/Evcher-arch/suunto-aqua-folder-picker/blob/codex/rustore-permissions/docs/rustore-permissions.md).
