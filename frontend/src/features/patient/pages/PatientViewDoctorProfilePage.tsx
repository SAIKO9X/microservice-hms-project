import { useParams } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import { useState } from "react";
import { toast } from "sonner";
import {
  Calendar,
  Clock,
  Award,
  Stethoscope,
  MessageSquare,
  Star,
} from "lucide-react";

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { StarRating } from "@/components/shared/StarRating";

import {
  getDoctorById,
  getDoctorStats,
  getDoctorReviews,
  getMyReviewForDoctor,
} from "@/services/profile";
import {
  useAppointmentsWithDoctorNames,
  useCreateAppointment,
} from "@/services/queries/appointment-queries";
import { ChatSheet } from "@/features/chat/components/ChatSheet";
import { CreateAppointmentDialog } from "@/features/patient/components/CreateAppointmentDialog";
import { CreateReviewDialog } from "@/features/patient/components/CreateReviewDialog";
import { resolveImageUrl } from "@/utils/media";

export const PatientViewDoctorProfilePage = () => {
  const { id } = useParams<{ id: string }>();
  const doctorId = Number(id);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [isAppointmentOpen, setIsAppointmentOpen] = useState(false);
  const [isReviewOpen, setIsReviewOpen] = useState(false);

  const { data: doctor, isLoading: isLoadingDoctor } = useQuery({
    queryKey: ["doctor", doctorId],
    queryFn: () => getDoctorById(doctorId),
    enabled: !!doctorId,
  });

  const { data: stats } = useQuery({
    queryKey: ["doctor-stats", doctorId],
    queryFn: () => getDoctorStats(doctorId),
    enabled: !!doctorId,
  });

  const { data: reviews } = useQuery({
    queryKey: ["doctor-reviews", doctorId],
    queryFn: () => getDoctorReviews(doctorId),
    enabled: !!doctorId,
  });

  const { data: myReview } = useQuery({
    queryKey: ["my-review", doctorId],
    queryFn: () => getMyReviewForDoctor(doctorId),
    enabled: !!doctorId,
  });

  const { data: appointments } = useAppointmentsWithDoctorNames();
  const createAppointmentMutation = useCreateAppointment();
  const completedAppointments = appointments?.filter(
    (a) => a.doctorId === doctorId && a.status === "COMPLETED",
  );

  const canReview =
    myReview || (completedAppointments && completedAppointments.length > 0);

  const reviewAppointmentId =
    myReview?.appointmentId ||
    (completedAppointments ? completedAppointments[0]?.id : 0);

  const handleAppointmentSubmit = (data: any) => {
    createAppointmentMutation.mutate(data, {
      onSuccess: () => {
        setIsAppointmentOpen(false);
        toast.success("Consulta agendada com sucesso!");
      },
      onError: (error: any) => {
        toast.error(
          error.response?.data?.message || "Erro ao agendar consulta.",
        );
      },
    });
  };

  if (isLoadingDoctor) {
    return (
      <div className="container py-10">
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  if (!doctor) {
    return <div className="container py-10">Médico não encontrado.</div>;
  }

  return (
    <div className="container mx-auto py-8 space-y-8">
      <Card className="border-none shadow-md bg-gradient-to-r from-blue-50 to-white dark:from-slate-900 dark:to-slate-950">
        <CardContent className="p-8 flex flex-col md:flex-row items-center md:items-start gap-8">
          <Avatar className="h-32 w-32 border-4 border-white shadow-lg">
            <AvatarImage
              src={resolveImageUrl(doctor.profilePictureUrl)}
              className="object-cover"
              alt="Foto de perfil do médico"
            />
            <AvatarFallback className="text-4xl">
              {doctor.name?.charAt(0)}
            </AvatarFallback>
          </Avatar>

          <div className="flex-1 text-center md:text-left space-y-2">
            <div className="flex flex-col md:flex-row items-center md:items-center gap-2">
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
                {doctor.name}
              </h1>
              <Badge className="bg-blue-600 hover:bg-blue-700">
                {doctor.specialization}
              </Badge>
            </div>

            <p className="text-muted-foreground flex items-center justify-center md:justify-start gap-2">
              <Stethoscope className="h-4 w-4" /> CRM: {doctor.crmNumber} •{" "}
              {doctor.department}
            </p>

            <div className="flex items-center justify-center md:justify-start gap-2 pt-1">
              <StarRating
                rating={stats?.averageRating || 0}
                readOnly
                size={20}
              />
              <span className="font-semibold text-lg">
                {stats?.averageRating?.toFixed(1)}
              </span>
              <span className="text-muted-foreground">
                ({stats?.totalReviews || 0} avaliações)
              </span>
            </div>
          </div>

          <div className="flex flex-col gap-3 min-w-[200px] pt-4">
            <Button
              className="px-8 shadow-md text-secondary"
              onClick={() => setIsAppointmentOpen(true)}
            >
              Agendar Consulta
            </Button>

            <Button
              variant="outline"
              className="w-full gap-2"
              onClick={() => setIsChatOpen(true)}
            >
              <MessageSquare className="w-4 h-4" />
              Enviar Mensagem
            </Button>

            {canReview && (
              <Button
                variant="secondary"
                className="w-full gap-2 bg-yellow-100 hover:bg-yellow-200 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400"
                onClick={() => setIsReviewOpen(true)}
              >
                <Star className="w-4 h-4" />
                {myReview ? "Editar Avaliação" : "Avaliar Médico"}
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-8">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <span className="text-primary">Sobre</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 text-gray-600 dark:text-gray-300 leading-relaxed">
              <p>
                {doctor.biography || "Nenhuma biografia informada pelo médico."}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Qualificações & Experiência</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-start gap-3">
                <Award className="h-5 w-5 text-primary mt-1" />
                <div>
                  <p className="font-semibold">Anos de Prática</p>
                  <p className="text-muted-foreground">
                    {doctor.yearsOfExperience} anos de experiência clínica.
                  </p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <Calendar className="h-5 w-5 text-primary mt-1" />
                <div>
                  <p className="font-semibold">Formação Acadêmica</p>
                  <p className="text-muted-foreground whitespace-pre-line">
                    {doctor.qualifications || "Não informado."}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Avaliações dos Pacientes</CardTitle>
            </CardHeader>
            <CardContent>
              {!reviews || reviews.length === 0 ? (
                <p className="text-muted-foreground py-4 text-center italic">
                  Este médico ainda não possui avaliações.
                </p>
              ) : (
                <div className="space-y-6">
                  {reviews.map((review) => (
                    <div
                      key={review.id}
                      className="border-b pb-6 last:border-0 last:pb-0"
                    >
                      <div className="flex justify-between items-start mb-2">
                        <div className="flex items-center gap-3">
                          <Avatar className="h-10 w-10">
                            <AvatarImage
                              src={resolveImageUrl(review.patientPhotoUrl)}
                              alt="Foto do paciente"
                            />
                            <AvatarFallback>
                              {review.patientName
                                ? review.patientName.charAt(0)
                                : "P"}
                            </AvatarFallback>
                          </Avatar>
                          <div>
                            <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                              {review.patientName || "Paciente Verificado"}
                            </p>
                            <div className="flex items-center gap-1 mt-0.5">
                              <StarRating
                                rating={review.rating}
                                readOnly
                                size={12}
                              />
                            </div>
                          </div>
                        </div>

                        <span className="text-xs text-muted-foreground flex items-center gap-1">
                          <Clock className="h-3 w-3" />
                          {format(new Date(review.createdAt), "dd MMM yyyy", {
                            locale: ptBR,
                          })}
                        </span>
                      </div>
                      <p className="text-sm text-gray-700 dark:text-gray-300 mt-3 pl-12">
                        {review.comment ? (
                          `"${review.comment}"`
                        ) : (
                          <span className="italic text-gray-400">
                            Avaliação sem comentário.
                          </span>
                        )}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card className="bg-blue-50 border-blue-100 dark:bg-blue-950/20 dark:border-blue-900">
            <CardHeader>
              <CardTitle className="text-blue-800 dark:text-blue-400">
                Por que escolher este médico?
              </CardTitle>
            </CardHeader>
            <CardContent className="text-sm text-blue-700 dark:text-blue-300 space-y-2 font-medium">
              <p>✓ Especialista verificado</p>
              <p>✓ Alta taxa de satisfação</p>
              <p>✓ Atendimento humanizado</p>
            </CardContent>
          </Card>
        </div>
      </div>

      <ChatSheet
        isOpen={isChatOpen}
        onOpenChange={setIsChatOpen}
        recipientId={doctor.userId}
        recipientName={doctor.name}
      />

      <CreateAppointmentDialog
        open={isAppointmentOpen}
        onOpenChange={setIsAppointmentOpen}
        onSubmit={handleAppointmentSubmit}
        isPending={createAppointmentMutation.isPending}
        defaultDoctorId={doctorId}
      />

      {reviewAppointmentId > 0 && (
        <CreateReviewDialog
          open={isReviewOpen}
          onOpenChange={setIsReviewOpen}
          appointmentId={reviewAppointmentId}
          doctorId={doctorId}
          doctorName={doctor.name}
        />
      )}
    </div>
  );
};
