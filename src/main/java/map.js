var myMap    = null;
    var geoObjs  = [];
    var infoBar  = document.getElementById('info-bar');
    var mapReady = false;

    ymaps.ready(function () {
        document.getElementById('loading').style.display = 'none';

        myMap = new ymaps.Map('map', {
            center: [55.76, 37.64],
            zoom: 5,
            controls: ['zoomControl', 'fullscreenControl', 'typeSelector']
        }, { suppressMapOpenBlock: true });

        mapReady = true;

        if (window._pendingRoute) {
            var p = window._pendingRoute;
            window._pendingRoute = null;
            buildRoute(p.from, p.to);
        }
    });

    function clearMap() {
        geoObjs.forEach(function (o) { myMap.geoObjects.remove(o); });
        geoObjs = [];
        infoBar.style.display = 'none';
    }

    window.showRoute = function (from, to) {
        if (!mapReady) {
            window._pendingRoute = { from: from, to: to };
            return;
        }
        buildRoute(from, to);
    };

    function buildRoute(from, to) {
    console.log("Запрос маршрута: " + from + " → " + to);
        clearMap();

        var coords = { from: false, to: false };

        function onBothReady() {
            if (coords.from === false || coords.to === false) return;

            if (coords.from) {
                var pmFrom = new ymaps.Placemark(coords.from, {
                    balloonContent: '<b>' + from + '</b>'
                }, {
                    preset: 'islands#blueCircleDotIconWithCaption',
                    iconCaptionMaxWidth: '200',
                    iconCaption: from
                });
                myMap.geoObjects.add(pmFrom);
                geoObjs.push(pmFrom);
            }

            if (coords.to) {
                var pmTo = new ymaps.Placemark(coords.to, {
                    balloonContent: '<b>' + to + '</b>'
                }, {
                    preset: 'islands#redCircleDotIconWithCaption',
                    iconCaptionMaxWidth: '200',
                    iconCaption: to
                });
                myMap.geoObjects.add(pmTo);
                geoObjs.push(pmTo);
            }

            if (coords.from && coords.to) {
                var line = new ymaps.Polyline(
                    [coords.from, coords.to], {},
                    { strokeColor: '#2563eb', strokeWidth: 4, strokeOpacity: 0.8, strokeStyle: 'dash' }
                );
                myMap.geoObjects.add(line);
                geoObjs.push(line);

                myMap.setBounds(myMap.geoObjects.getBounds(), {
                    checkZoomRange: true, zoomMargin: 100
                });

                var dist = Math.round(ymaps.coordSystem.geo.getDistance(coords.from, coords.to) / 1000);
                infoBar.innerHTML = '<b>' + from + '</b> \u2192 <b>' + to + '</b> &nbsp;|&nbsp; ~' + dist + ' \u043a\u043c (\u043f\u043e \u043f\u0440\u044f\u043c\u043e\u0439)';
                infoBar.style.display = 'block';
                console.log("Ответ Яндекс Геокодер: маршрут построен, расстояние ~" + dist + " км");

            } else if (coords.from || coords.to) {
                myMap.setCenter(coords.from || coords.to, 8);
                infoBar.innerHTML = '\u041d\u0430\u0439\u0434\u0435\u043d\u0430 \u0442\u043e\u043b\u044c\u043a\u043e \u043e\u0434\u043d\u0430 \u0442\u043e\u0447\u043a\u0430: <b>' + (coords.from ? from : to) + '</b>';
                infoBar.style.display = 'block';
            } else {
                infoBar.innerHTML = '\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043d\u0430\u0439\u0442\u0438: <b>' + from + '</b> \u0438 <b>' + to + '</b>';
                infoBar.style.display = 'block';
            }
        }

        ymaps.geocode(from, { results: 1 }).then(function (res) {
            var obj = res.geoObjects.get(0);
            coords.from = obj ? obj.geometry.getCoordinates() : null;
            onBothReady();
        }, function () { coords.from = null; onBothReady(); });

        ymaps.geocode(to, { results: 1 }).then(function (res) {
            var obj = res.geoObjects.get(0);
            coords.to = obj ? obj.geometry.getCoordinates() : null;
            onBothReady();
        }, function () { coords.to = null; onBothReady(); });
    }