import { useRef, useState } from "react";
import { Edit } from "lucide-react";
import { type PatientProfile, BloodGroup, Gender } from "@/types/patient.types";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ProfileInfoTable } from "@/features/patient/components/ProfileInfoTable";
import type { PatientProfileFormData } from "@/schemas/profile.schema";
import { EditProfileDialog } from "@/features/patient/components/EditProfileDialog";
import { CustomNotification } from "@/components/notifications/CustomNotification";
import {
  useProfile,
  useUpdateProfilePicture,
} from "@/services/queries/profile-queries";
import { uploadFile } from "@/services/media";

export const PatientProfilePage = () => {
  const {
    profile,
    status,
    user,
    isLoading,
    isError,
    error,
    updateProfile,
    isUpdating,
  } = useProfile();

  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [actionNotification, setActionNotification] = useState<{
    variant: "success" | "error";
    title: string;
  } | null>(null);

  const updatePictureMutation = useUpdateProfilePicture();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleSaveProfile = async (data: PatientProfileFormData) => {
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

  if (user?.role !== "PATIENT") {
    return (
      <div className="text-center p-10 text-red-500">
        Acesso negado. Esta página é apenas para pacientes.
      </div>
    );
  }

  if (isLoading) {
    return <div className="text-center p-10">Carregando perfil...</div>;
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

  // perfil não encontrado
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

  // verifica se profile é do tipo PatientProfile
  const patientProfile = profile as PatientProfile;

  const isProfileIncomplete =
    patientProfile &&
    (!patientProfile.phoneNumber ||
      !patientProfile.address ||
      !patientProfile.dateOfBirth);

  const personalInfoData = [
    { label: "CPF", value: patientProfile?.cpf || "Não informado" },
    {
      label: "Data de Nascimento",
      value: patientProfile?.dateOfBirth
        ? new Date(patientProfile.dateOfBirth).toLocaleDateString("pt-BR")
        : "Não informado",
    },
    {
      label: "Telefone",
      value: patientProfile?.phoneNumber || "Não informado",
    },
    { label: "Endereço", value: patientProfile?.address || "Não informado" },
  ];

  const medicalInfoData = [
    {
      label: "Tipo Sanguíneo",
      value: patientProfile?.bloodGroup
        ? BloodGroup[patientProfile.bloodGroup] || patientProfile.bloodGroup
        : "Não informado",
    },
    {
      label: "Gênero",
      value: patientProfile?.gender
        ? Gender[patientProfile.gender] || patientProfile.gender
        : "Não informado",
    },
  ];

  const emergencyContactData = [
    {
      label: "Nome",
      value: patientProfile?.emergencyContactName || "Não informado",
    },
    {
      label: "Telefone",
      value: patientProfile?.emergencyContactPhone || "Não informado",
    },
  ];

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

  const API_BASE_URL = "http://localhost:9000"; // URL do Gateway

  return (
    <div className="container mx-auto p-4 space-y-8">
      {/* aviso para perfil incompleto */}
      {isProfileIncomplete && (
        <CustomNotification
          variant="info"
          title="Complete seu Perfil - Clique em 'Editar Perfil'"
          description="Seu perfil foi criado com sucesso! Complete suas informações pessoais para poder usar todos os recursos disponíveis."
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
                  src={
                    patientProfile?.profilePictureUrl
                      ? patientProfile.profilePictureUrl.startsWith("http")
                        ? patientProfile.profilePictureUrl
                        : `${API_BASE_URL}${patientProfile.profilePictureUrl}`
                      : undefined
                  }
                  alt="Foto do perfil"
                />
                <AvatarFallback className="text-3xl">
                  {user?.name?.charAt(0).toUpperCase() || "P"}
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
            <div className="flex-1 text-center sm:text-left">
              <CardTitle className="text-2xl">
                {user?.name || "Nome não informado"}
              </CardTitle>
              <p className="text-muted-foreground">{user?.email}</p>
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
            title="Contato de Emergência"
            data={emergencyContactData}
          />
        </div>

        <div className="space-y-8">
          <ProfileInfoTable
            title="Informações Médicas Básicas"
            data={medicalInfoData}
          />
          <Card>
            <CardHeader>
              <CardTitle>Alergias</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-wrap gap-2">
              {patientProfile?.allergies &&
              patientProfile.allergies.length > 0 ? (
                (typeof patientProfile.allergies === "string"
                  ? patientProfile.allergies.split(",")
                  : patientProfile.allergies
                ).map((item: string, index: number) => (
                  <Badge key={index} variant="secondary">
                    {item.trim()}
                  </Badge>
                ))
              ) : (
                <p className="text-sm text-muted-foreground">
                  Nenhuma alergia registrada.
                </p>
              )}
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Doenças Crônicas</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-wrap gap-2">
              {patientProfile?.chronicDiseases &&
              patientProfile.chronicDiseases.length > 0 ? (
                (typeof patientProfile.chronicDiseases === "string"
                  ? patientProfile.chronicDiseases.split(",")
                  : patientProfile.chronicDiseases
                ).map((item: string, index: number) => (
                  <Badge key={index} variant="secondary">
                    {item.trim()}
                  </Badge>
                ))
              ) : (
                <p className="text-sm text-muted-foreground">
                  Nenhuma doença crônica registrada.
                </p>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      {/* --- DIALOG DE EDIÇÃO --- */}
      {patientProfile && (
        <EditProfileDialog
          open={isDialogOpen}
          onOpenChange={setIsDialogOpen}
          profile={patientProfile}
          onSave={handleSaveProfile}
        />
      )}
    </div>
  );
};
