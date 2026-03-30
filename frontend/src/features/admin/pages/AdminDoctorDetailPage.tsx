import { useParams, Link, useNavigate } from "react-router";
import { useDoctorById } from "@/services/queries/profile-queries";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  ArrowLeft,
  Phone,
  Award,
  Briefcase,
  GraduationCap,
} from "lucide-react";

export const AdminDoctorDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const { data: doctor, isLoading, isError } = useDoctorById(Number(id));
  const navigate = useNavigate();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center space-y-2">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
          <p className="text-muted-foreground">
            Carregando perfil do médico...
          </p>
        </div>
      </div>
    );
  }

  if (isError || !doctor) {
    return (
      <div className="container mx-auto py-8">
        <Button asChild variant="outline" className="mb-4">
          <Link to="/admin/users">
            <ArrowLeft className="mr-2 h-4 w-4" />
            Voltar para a lista
          </Link>
        </Button>
        <Card className="border-destructive">
          <CardContent className="pt-6">
            <div className="text-center text-destructive">
              <p className="text-lg font-semibold">Erro ao carregar perfil</p>
              <p className="text-sm text-muted-foreground mt-2">
                Não foi possível encontrar os dados deste médico.
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-8 space-y-6">
      {/* Cabeçalho com botão de voltar */}
      <div className="flex items-center gap-4">
        <Button asChild variant="outline" size="icon">
          <Link to="/admin/users">
            <ArrowLeft className="h-4 w-4" />
          </Link>
        </Button>
        <div>
          <h1 className="text-3xl font-bold">Perfil do Médico</h1>
          <p className="text-muted-foreground">
            Visualização completa das informações profissionais
          </p>
        </div>
      </div>

      {/* Card de Informações Principais */}
      <Card>
        <CardHeader className="pb-4">
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-4">
              <div className="h-20 w-20 rounded-full bg-primary/10 flex items-center justify-center">
                <span className="text-3xl font-bold text-primary">
                  {doctor.name.charAt(0).toUpperCase()}
                </span>
              </div>
              <div>
                <CardTitle className="text-2xl">{doctor.name}</CardTitle>
                <div className="flex items-center gap-2 mt-2">
                  <Badge
                    variant="outline"
                    className="bg-blue-50 text-blue-700 border-blue-200"
                  >
                    CRM: {doctor.crmNumber}
                  </Badge>
                  {doctor.specialization && (
                    <Badge
                      variant="outline"
                      className="bg-purple-50 text-purple-700 border-purple-200"
                    >
                      {doctor.specialization}
                    </Badge>
                  )}
                </div>
              </div>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Grid de Informações */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Informações Pessoais e Profissionais */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Briefcase className="h-5 w-5" />
              Resumo Profissional
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {doctor.department && (
              <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                <Briefcase className="h-5 w-5 text-muted-foreground mt-0.5" />
                <div className="flex-1">
                  <p className="text-sm font-medium text-muted-foreground">
                    Departamento
                  </p>
                  <p className="text-base font-semibold">{doctor.department}</p>
                </div>
              </div>
            )}
            {doctor.yearsOfExperience !== undefined && (
              <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                <Award className="h-5 w-5 text-muted-foreground mt-0.5" />
                <div className="flex-1">
                  <p className="text-sm font-medium text-muted-foreground">
                    Experiência
                  </p>
                  <p className="text-base font-semibold">
                    {doctor.yearsOfExperience} anos
                  </p>
                </div>
              </div>
            )}
            {doctor.phoneNumber && (
              <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                <Phone className="h-5 w-5 text-muted-foreground mt-0.5" />
                <div className="flex-1">
                  <p className="text-sm font-medium text-muted-foreground">
                    Telefone
                  </p>
                  <p className="text-base font-semibold">
                    {doctor.phoneNumber}
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Qualificações */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <GraduationCap className="h-5 w-5" />
              Qualificações
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm leading-relaxed whitespace-pre-wrap text-muted-foreground">
              {doctor.qualifications || "Nenhuma qualificação informada."}
            </p>
          </CardContent>
        </Card>

        {/* Biografia */}
        {doctor.biography && (
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Biografia</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm leading-relaxed whitespace-pre-wrap text-muted-foreground">
                {doctor.biography}
              </p>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Ações */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col sm:flex-row gap-3">
            <Button
              variant="outline"
              className="flex-1"
              onClick={() =>
                navigate(`/admin/users/doctor/${doctor.id}/schedule`)
              }
            >
              Ver Agenda do Médico
            </Button>

            <Button
              variant="outline"
              className="flex-1"
              onClick={() =>
                navigate(`/admin/users/doctor/${doctor.id}/history`)
              }
            >
              Ver Histórico de Consultas
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
