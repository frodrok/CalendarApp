if (window.console) {
  console.log("app init, lets go");
}

function setupCalendar(calendarObject, events) {
  if (events.length < 1) {
    console.log("no events recieved");
  } else {
    console.log("setting up calendar with these events: ");
    events.forEach(console.log.bind(console));
  }

  $('#calendar').fullCalendar({
    header: {
      left: 'prev,next today',
      center: 'title',
      right: 'month,basicWeek,basicDay'
    },
    defaultDate: '2016-08-01',
    editable: false,
    eventLimit: true,
    events: events
  });
}

function setupNewCalendar(calendarObject, events) {

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

    calendarObject.fullCalendar({
        header: {
            left: 'prev,next today',
            center: 'title',
            right: 'month,basicWeek,basicDay'
        },
        defaultDate: '2016-08-06',
        editable: true,
        eventLimit: true,
        events: fixedEvents
    });
}