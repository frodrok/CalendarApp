var url = "http://83.227.87.14:9000";

function handleMenu(userId, groupId, menuElements, content, calendar, calendarEditable, admin) {

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

                var underlingsWrapper = "<div id='underlings'>" + underlingsHtml + "</div>";

                content.html(underlingsWrapper);

                var addUnderlingForm = "<form id='addunderling'><input type='text' name='username'><input type='password' name='password'><input type='submit' value='Add underling'></form>";
                $("#underlings").after(addUnderlingForm);

                $("#addunderling").submit(function(e) {
                    e.preventDefault();
                    var username = $("#addunderling input[name='username']").val();
                    var password = $("#addunderling input[name='password']").val();

                    addUnderlingFunction(username, password, function(e) {
                        console.log(e);
                       window.location.href = "/user";
                    });
                });
            });


        } else if (clicked == "Calendar") {
            var emptyCal = "<div id='calendar'></div>";
            // content.html('');
            content.append(emptyCal);

            $("#underlings").remove();
            $("#addunderling").remove();

            getEvents(userId, function (data) {
                setupCalendar($("#calendar"), data, calendarEditable, admin);
            });
        } else if (clicked == "Administrator") {
        } else {
            console.log("unhandled menu element");
        }


    });
}

function addUnderlingFunction(username, password, callback) {

    getLicenseIdByUserId(function(e) {
        var data = {"username": username, "password": password, "admin": false, "groupId": groupId, "superAdmin": false, "licenseId": e};

        var asJson = $.toJSON(data);
        console.log(asJson);

        $.ajax({
            type: "POST",
            url: api_url + "/users",
            data: asJson,
            contentType: "application/json",
            success: function(e) {
                callback(e);
            }
        }).fail(function(e) {
            callback(e);
        });

    });



}

function getLicenseIdByUserId(callback) {
    $.get(api_url + "/users/" + userId, function(data) {
       callback(data.licenseId);
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