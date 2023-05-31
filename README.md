# SICompressor
Простое приложение для сжатия паков игры Владимира Хиля SIGame - https://vladimirkhil.com/si/game

Помогает с загрузкой медиа во время игры, к сожалению не все авторы паков достаточно внимательно подходят к добавлению изображений, музыки и видео. Часто у части игроков не загружаются картинки или загружаются с сильной задержкой. Зависимость со скоростью интернета игроков при этом не очевидная.

По опыту - зашакалить медиа помогает, но в 100% случаев. Конкретных тестов не проводилось. Для лучшего опыта игры можно помочь Владимиру Хилю организовать доставку пакетов до игроков 😉.

# Настройки
В настройках есть возможность включить аппаратное ускорение на видеокарте: 
* h264_nvenc - для nvidia
* h264_amf - для AMD
* libx264 - если не уверены, будет работать на процессоре

Я постарался сделать так чтобы паки не ломались, __по идее__ в худшем случае файл просто не сожмется. Но вы всегда можете проверит результат сами:
- Распаковать пакет и проверить файлы
- Открыть пакет в SIQuester https://vladimirkhil.com/si/siquester

# Спасибо Владимир Хиль 
Формат у паков достаточно простой и описан тут - https://github.com/VladimirKhil/SI/wiki/Спецификация-формата-.siq
За это описание и отличную платформу онлайн игр спасибо Владимиру Хилю 
