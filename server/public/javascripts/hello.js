if (window.console) {
  console.log("app init, lets go");
}

function setupCalendar(calendarObject, events) {

    /* wierd mapping one variable name */
    /* jquery fullcalendar event object example:
        {id: 999, title: 'repeating event', start: 20160806T14:37', end: '20160806T20:00',
         allDay: false, url: 'http://google.se'}
     */
    var fixedEvents = events.map(function(event) {
        return {id: event.id, title: event.title, start: event.from}
    });

    if (events.length < 1) {
        console.log("no events recieved");
    }

    var today = new Date()
    console.log(today.toTimeString());

    calendarObject.fullCalendar({
        header: {
            left: 'prev,next today',
            center: 'title',
             /*right: 'month,basicWeek,basicDay'*/
            right: 'month,agendaWeek,agendaDay'
        },
        weekends: false,
        fixedWeekCount: false,
        selectable: true,
        selectHelper: true,
        select: function(start, end, allDay) {
            var title = prompt('Event Title:');
            /*
             if title is enterd calendar will add title and event into fullCalendar.
             */
            if (title)
            {
                calendarObject.fullCalendar('renderEvent',
                    {
                        title: title,
                        start: start,
                        end: end,
                        allDay: allDay
                    },
                    true // make the event "stick"
                );
            }
            calendarObject.fullCalendar('unselect');
        },
        defaultDate: '2016-08-06',
        editable: true,
        eventLimit: true,
        events: fixedEvents
    });
}