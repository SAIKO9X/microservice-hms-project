import { useRef, useState } from "react";
import { Edit } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ProfileInfoTable } from "@/features/patient/components/ProfileInfoTable";
import { EditDoctorProfileDialog } from "@/features/doctor/components/EditDoctorProfileDialog";
import { CustomNotification } from "@/components/notifications/CustomNotification";
import { StarRating } from "@/components/shared/StarRating";
import {
  useProfile,
  useUpdateProfilePicture,
} from "@/services/queries/profile-queries";
import { uploadFile } from "@/services/media";
import { getDoctorReviews, getDoctorStats } from "@/services/profile";
import type { DoctorProfile } from "@/types/doctor.types";
import type { DoctorProfileFormData } from "@/schemas/profile.schema";
import { resolveImageUrl } from "@/utils/media";

export const DoctorProfilePage = () => {
  const {
    profile,
    status,
    error,
    user,
    isLoading,
    isError,
    updateProfile,
    isUpdating,
  } = useProfile();

  const updatePictureMutation = useUpdateProfilePicture();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [actionNotification, setActionNotification] = useState<{
    variant: "success" | "error";
    title: string;
  } | null>(null);

  const doctorId = profile?.id;

  const { data: stats } = useQuery({
    queryKey: ["doctor-stats", doctorId],
    queryFn: () => getDoctorStats(doctorId!),
    enabled: !!doctorId,
  });

  const { data: reviews } = useQuery({
    queryKey: ["doctor-reviews", doctorId],
    queryFn: () => getDoctorReviews(doctorId!),
    enabled: !!doctorId,
  });

  const handleSaveProfile = async (data: DoctorProfileFormData) => {
    try {
      await updateProfile(data);
      setIsDialogOpen(false);
      setActionNotification({
        variant: "success",
        title: "Perfil atualizado com sucesso!",
      });
    } catch (err: any) {
      setActionNotification({
        variant: "error",
        title: err.message || "Não foi possível salvar as alterações.",
      });
    }
  };

  const handleFileChange = async (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];
    if (!file) return;

    try {
      const mediaResponse = await uploadFile(file);
      await updatePictureMutation.mutateAsync(mediaResponse.url);
      setActionNotification({
        variant: "success",
        title: "Foto de perfil atualizada com sucesso!",
      });
    } catch (err: any) {
      setActionNotification({
        variant: "error",
        title: "Erro ao atualizar a foto",
      });
    }
  };

  if (user?.role !== "DOCTOR") {
    return (
      <div className="text-center p-10 text-red-500">
        Acesso negado. Esta página é apenas para doutores.
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="text-center p-10">Carregando perfil do doutor...</div>
    );
  }

  if (isError) {
    return (
      <div className="container mx-auto p-4 text-center">
        <CustomNotification
          variant="error"
          title={error || "Erro ao carregar perfil"}
        />
        <Button onClick={() => window.location.reload()} className="mt-4">
          Tentar Novamente
        </Button>
      </div>
    );
  }

  if (status === "succeeded" && !profile) {
    return (
      <div className="container mx-auto p-4">
        <CustomNotification
          variant="info"
          title="Perfil não encontrado"
          description="Não foi possível encontrar seu perfil. Entre em contato com o suporte."
        />
      </div>
    );
  }

  const doctorProfile = profile as DoctorProfile;

  const isProfileIncomplete =
    doctorProfile && !doctorProfile.specialization && !doctorProfile.department;

  const professionalInfoData = [
    { label: "CRM", value: doctorProfile?.crmNumber || "Não informado" },
    {
      label: "Especialização",
      value: doctorProfile?.specialization || "Não informado",
    },
    {
      label: "Departamento",
      value: doctorProfile?.department || "Não informado",
    },
    {
      label: "Anos de Experiência",
      value: doctorProfile?.yearsOfExperience
        ? `${doctorProfile.yearsOfExperience} anos`
        : "Não informado",
    },
    { label: "Telefone", value: doctorProfile?.phoneNumber || "Não informado" },
  ];

  const personalInfoData = [
    {
      label: "Nome",
      value: user?.name || "Não informado",
    },
    {
      label: "Data de Nascimento",
      value: doctorProfile?.dateOfBirth
        ? new Date(doctorProfile.dateOfBirth).toLocaleDateString("pt-BR")
        : "Não informado",
    },
  ];

  return (
    <div className="container mx-auto p-4 space-y-8">
      {isProfileIncomplete && (
        <CustomNotification
          variant="info"
          title="Complete seu Perfil - Clique em Editar Perfil"
          description="Seu perfil foi criado com sucesso! Complete suas informações profissionais para acessar todas as funcionalidades."
          dismissible={false}
        />
      )}

      {actionNotification && (
        <CustomNotification
          variant={actionNotification.variant}
          title={actionNotification.title}
          autoHide
          onDismiss={() => setActionNotification(null)}
        />
      )}

      <Card>
        <CardHeader>
          <div className="flex flex-col sm:flex-row items-center gap-6">
            <div className="relative group">
              <Avatar className="h-24 w-24">
                <AvatarImage
                  src={resolveImageUrl(doctorProfile?.profilePictureUrl)}
                  alt="Foto de perfil"
                />
                <AvatarFallback className="text-3xl">
                  {user?.name?.charAt(0).toUpperCase() || "D"}
                </AvatarFallback>
              </Avatar>
              <Button
                size="sm"
                variant="outline"
                className="absolute bottom-0 right-0 opacity-0 group-hover:opacity-100 transition-opacity"
                onClick={() => fileInputRef.current?.click()}
                disabled={updatePictureMutation.isPending}
              >
                <Edit className="h-3 w-3" />
                <span className="sr-only">Editar foto</span>
              </Button>
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileChange}
                className="hidden"
                accept="image/png, image/jpeg, image/gif"
              />
            </div>

            <div className="flex-1 text-center sm:text-left space-y-1">
              <CardTitle className="text-2xl">
                {user?.name || "Nome não informado"}
              </CardTitle>
              <p className="text-muted-foreground">
                {doctorProfile?.specialization ||
                  "Especialização não informada"}
              </p>
              <p className="text-sm text-muted-foreground">
                CRM: {doctorProfile?.crmNumber || "Não informado"}
              </p>

              <div className="flex items-center gap-2 justify-center sm:justify-start pt-1">
                <StarRating rating={stats?.averageRating || 0} readOnly />
                <span className="text-sm text-muted-foreground">
                  ({stats?.totalReviews || 0} avaliações)
                </span>
              </div>
            </div>

            <Button
              variant="outline"
              onClick={() => setIsDialogOpen(true)}
              disabled={isUpdating}
            >
              <Edit className="h-4 w-4 mr-2" />
              {isUpdating ? "Salvando..." : "Editar Perfil"}
            </Button>
          </div>
        </CardHeader>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
        <div className="space-y-8">
          <ProfileInfoTable
            title="Informações Pessoais"
            data={personalInfoData}
          />
          <ProfileInfoTable
            title="Informações Profissionais"
            data={professionalInfoData}
          />
        </div>

        <div className="space-y-8">
          <Card>
            <CardHeader>
              <CardTitle>Biografia</CardTitle>
            </CardHeader>
            <CardContent className="text-sm text-muted-foreground whitespace-pre-wrap">
              {doctorProfile?.biography || "Nenhuma biografia informada."}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Qualificações</CardTitle>
            </CardHeader>
            <CardContent className="text-sm text-muted-foreground whitespace-pre-wrap">
              {doctorProfile?.qualifications ||
                "Nenhuma qualificação informada."}
            </CardContent>
          </Card>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Avaliações dos Pacientes</CardTitle>
        </CardHeader>
        <CardContent>
          {!reviews || reviews.length === 0 ? (
            <p className="text-muted-foreground text-center py-4">
              Ainda não existem avaliações com comentários.
            </p>
          ) : (
            <div className="space-y-6">
              {reviews.map((review) => (
                <div
                  key={review.id}
                  className="border-b pb-4 last:border-0 last:pb-0"
                >
                  <div className="flex justify-between items-center mb-2">
                    <StarRating rating={review.rating} readOnly size={16} />
                    <span className="text-xs text-muted-foreground">
                      {new Date(review.createdAt).toLocaleDateString("pt-BR")}
                    </span>
                  </div>
                  <p className="text-sm text-foreground italic">
                    "{review.comment || "Sem comentário escrito."}"
                  </p>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <EditDoctorProfileDialog
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
        profile={doctorProfile}
        onSave={handleSaveProfile}
      />
    </div>
  );
};
