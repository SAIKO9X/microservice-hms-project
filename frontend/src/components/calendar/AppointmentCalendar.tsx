import { useState } from "react";
import { Calendar, dateFnsLocalizer } from "react-big-calendar";
import type { Event, EventProps, View } from "react-big-calendar";
import withDragAndDrop from "react-big-calendar/lib/addons/dragAndDrop";
import type { withDragAndDropProps } from "react-big-calendar/lib/addons/dragAndDrop";
import { format, parse, startOfWeek, getDay } from "date-fns";
import { ptBR } from "date-fns/locale";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

import "react-big-calendar/lib/css/react-big-calendar.css";
import "react-big-calendar/lib/addons/dragAndDrop/styles.css";

const locales = { "pt-BR": ptBR };
const localizer = dateFnsLocalizer({
  format,
  parse,
  startOfWeek,
  getDay,
  locales,
});

const DnDCalendar = withDragAndDrop<AppointmentEvent>(Calendar);

export interface AppointmentEvent extends Event {
  id: number;
  patientName: string;
  status: string;
  reason?: string;
}

interface AppointmentCalendarProps {
  events: AppointmentEvent[];
  onEventReschedule: (
    appointmentId: number,
    newStart: Date,
    newEnd: Date,
  ) => void;
  onEventClick?: (event: AppointmentEvent) => void;
}

const CustomToolbar = (toolbar: any) => {
  const goToBack = () => toolbar.onNavigate("PREV");
  const goToNext = () => toolbar.onNavigate("NEXT");
  const goToCurrent = () => toolbar.onNavigate("TODAY");

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between mb-4 gap-4">
      <div className="flex items-center gap-2">
        <Button variant="outline" size="sm" onClick={goToCurrent}>
          Hoje
        </Button>
        <div className="flex items-center gap-1 bg-muted/50 rounded-md p-1 border">
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={goToBack}
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={goToNext}
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
        <span className="text-xl font-bold text-foreground capitalize ml-2">
          {toolbar.label}
        </span>
      </div>

      <div className="flex items-center bg-muted/50 rounded-md p-1 border">
        {["month", "work_week", "day"].map((viewName) => (
          <Button
            key={viewName}
            variant={toolbar.view === viewName ? "default" : "ghost"}
            size="sm"
            className="h-7 px-3 text-xs"
            onClick={() => toolbar.onView(viewName)}
          >
            {viewName === "month"
              ? "Mês"
              : viewName === "work_week"
                ? "Semana"
                : "Dia"}
          </Button>
        ))}
      </div>
    </div>
  );
};

const CustomEvent = ({ event }: EventProps<AppointmentEvent>) => (
  <div className="flex flex-col p-1 h-full">
    <span className="text-sm font-semibold truncate leading-tight">
      {event.patientName}
    </span>
    <span className="text-xs opacity-90 truncate">
      {event.reason || "Consulta"}
    </span>
  </div>
);

export function AppointmentCalendar({
  events,
  onEventReschedule,
  onEventClick,
}: AppointmentCalendarProps) {
  const [view, setView] = useState<View>("work_week");
  const [date, setDate] = useState<Date>(new Date());

  const onEventDrop: withDragAndDropProps<AppointmentEvent>["onEventDrop"] = ({
    event,
    start,
    end,
  }) => {
    if (event.status !== "SCHEDULED") return;

    if (start && end) {
      onEventReschedule(event.id, new Date(start), new Date(end));
    }
  };

  const eventStyleGetter = (event: AppointmentEvent) => {
    let backgroundColor = "var(--primary)";

    if (event.status === "COMPLETED") backgroundColor = "#10b981";
    if (event.status === "CANCELLED" || event.status === "NO_SHOW")
      backgroundColor = "#ef4444";

    return {
      style: {
        backgroundColor,
        color: "var(--primary-foreground)",
        border: `1px solid ${backgroundColor}`,
      },
    };
  };

  return (
    <div className="h-[700px] w-full bg-card rounded-xl shadow-sm border p-4">
      <DnDCalendar
        localizer={localizer}
        events={events}
        onEventDrop={onEventDrop}
        onSelectEvent={(e) => onEventClick?.(e)}
        resizable={false}
        view={view}
        onView={setView}
        date={date}
        onNavigate={setDate}
        views={["month", "work_week", "day"]}
        step={30}
        timeslots={2}
        culture="pt-BR"
        min={new Date(2024, 1, 1, 7, 0, 0)}
        max={new Date(2024, 1, 1, 20, 0, 0)}
        eventPropGetter={eventStyleGetter}
        components={{
          toolbar: CustomToolbar,
          event: CustomEvent,
        }}
        messages={{
          allDay: "Dia todo",
          noEventsInRange: "Nenhuma consulta neste período.",
          showMore: (total) => `+${total} mais`,
        }}
      />
    </div>
  );
}
