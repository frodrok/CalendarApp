var fCalendar = null;

var eventsToBeRemoved = [];

function setupCalendar(calendarObject, events, editable) {

    /* wierd mapping one variable name */
    /* jquery fullcalendar event object example:
        {id: 999, title: 'repeating event', start: 20160806T14:37', end: '20160806T20:00',
         allDay: false, url: 'http://google.se', rendering: 'background'}
     */
    var fixedEvents = events.map(function(event) {
        if (event.background) {
            return {
                    id: event.id,
                    title: event.title,
                    start: event.from,
                    end: event.to,
                    rendering: 'background',
                    allDay: true,
                    backgroundColor: 'green',
                    color: event.color
                }

        } else {
            return {id: event.id, title: event.title, start: event.from, end: event.to, color: event.color}
        }
    });

    /* 2016-08-07T14:20:00 */

    if (events.length < 1) {
        console.log("no events recieved");
    }

    fCalendar = calendarObject.fullCalendar({
        header: {
            left: 'prev,next today addEvent saveEvents',
            center: 'title',
             /*right: 'month,basicWeek,basicDay'*/
            right: 'month,agendaWeek,agendaDay'
        },
        weekends: true,
        fixedWeekCount: false,
        selectable: true,
        selectHelper: true,
        /* select: function(start, end, allDay) {

            console.log("in select start: " + start.format('YYYY-MM-DD hh:mm'));
            console.log("in select end: " + end.format('YYYY-MM-DD hh:mm'));

            showNewEventWindowAndGetVariables(function(e) {
                var eventTitle = e.title;
                var fromValue = e.fromInput;
                var toValue = e.toInput;
                var selectedStart = moment(new Date(start)).format('YYYY-MM-DD');
                /* var selectedEnd = moment(new Date(end)).format('YYYY-MM-DD');
                var selectedEnd = end.format('YYYY-MM-DD');

                var totalStart = selectedStart + "T" + fromValue + ":00";
                var totalEnd = selectedEnd + "T" + toValue + ":00";
                addEventToCalendar(calendarObject, eventTitle, totalStart, totalEnd, allDay);
            });

            /*
             if title is enterd calendar will add title and event into fullCalendar.
             */
            /* if (title)
            {
                calendarObject.fullCalendar('renderEvent',
                    {
                        title: title,
                        start: moment(new Date(start)).format(),
                        end: end,
                        allDay: allDay
                    },
                    true // make the event "stick"
                );
            }
            calendarObject.fullCalendar('unselect');
        }, */
        eventClick: function(event, jsEvent, view) {
          toggleClick(event, jsEvent, view);
        },
        defaultDate: '2016-08-07',
        editable: editable,
        timeFormat: 'H:mm',
        eventLimit: true,
        aspectRatio: 2,
        customButtons: {
            addEvent: {
                text: 'Add event',
                click: function(e) {
                    $("#neweventmodal").modal('show');
                }
            },
            saveEvents: {
                text: 'Save all events',
                click: function(e) {
                    saveEventsButtonClick();
                }
            }
        },
        events: fixedEvents,
        eventRender: function (event, element) {
            if (event.rendering == 'background') {
                element.append(event.title);
            }
        }
    });
}

function addEventToCalendar(calendarObject, title, start, end, allDay) {
    console.log("adding event: " + start + " " + end);
    calendarObject.fullCalendar('renderEvent',
        {
            title: title,
            start: start,
            end: end,
            allDay: false
        },
        true
    );

    calendarObject.fullCalendar('unselect');
}

var toggled = 0;

function toggleClick(event, jsEvent, view) {
    var htmlTarget = jsEvent.currentTarget;

    var newDiv = "<div id='deletetoggled' class='btn btn-primary' style='display: inline'><span>Delete</span></div>";

    if (toggled == 0) {
        $(htmlTarget).css("background-color", "red");
        $("#manipulateevents div:last-child").after(newDiv);
        toggled = 1;

        $("#deletetoggled").click(function(e) {
            eventsToBeRemoved.push(event.id);
            console.log(eventsToBeRemoved);
           fCalendar.fullCalendar('removeEvents', event.id);
            fCalendar.fullCalendar('rerenderEvents');
            $("#deletetoggled").remove();
        });
    } else {
        $("#deletetoggled").remove();
        $(htmlTarget).css("background-color", "#3A87AD");
        toggled = 0;
    }


}

function getDeletedEvents() {
    return eventsToBeRemoved;
}

function showNewEventWindowAndGetVariables(callback) {

    /* var title = prompt('Event Title:', event.title, { buttons: { Ok: true, Cancel: false} }); */

    var neweventmodal = $('#neweventmodal').modal('show');
    var neweventform = $("#newEventForm");

    $('button#saveEventButton').click(function(e) {
        e.preventDefault();

        var from = $("#newEventForm input[name='from']").val();
        var to = $("#newEventForm input[name='to']").val();
        var title = $("#newEventForm input[name='title']").val();

        neweventmodal.modal('hide');
        neweventform[0].reset();

        callback({title: title, fromInput: from, toInput: to})

    });

}