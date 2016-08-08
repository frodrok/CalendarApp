var url = "http://83.227.87.14:9000";

function handleMenu(userId, groupId, menuElements, content, calendar, calendarEditable) {

    menuElements.click(function (e) {

        e.preventDefault();

        $.each(menuElements, function() {
            $(this).removeClass("active");
        });

        $(this).addClass("active");

        var clicked = $(this).text();

        if (clicked == "Underlings") {
            getUnderlings(groupId, function (data) {
                var underlingsHtml = data.map(function (obj) {
                    return "<p>id: " + obj.id + ", username: " + obj.username + "</p>";
                });

                content.html(underlingsHtml);
            });
        } else if (clicked == "Calendar") {
            var emptyCal = "<div id='calendar'></div>";
            content.append(emptyCal);

            getEvents(userId, function (data) {
                setupCalendar($("#calendar"), data, calendarEditable);
            });
        } else if (clicked == "Administrator") {
        } else {
            console.log("unhandled menu element");
        }


    });
}

function getEvents(userId, callback) {
    $.get( url + "/users/" + userId + "/events", function(data) {
        callback(data);
    }).fail(function(error) {
        callback(error);
    });
//                $.get(url + "/users/" + userId + "/events",  )
}

function getUnderlings(groupId, callback) {
    $.get(url + "/groups/" + groupId + "/users", function (data) {
        var underlings = data.filter(function (object) {
            return object.admin == false;
        });

        callback(underlings);
    });
}