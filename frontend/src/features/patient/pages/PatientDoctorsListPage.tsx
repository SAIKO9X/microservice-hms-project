import { useState, useMemo } from "react";
import { Link } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { Search, Stethoscope, Users, SlidersHorizontal, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuCheckboxItem,
  DropdownMenuTrigger,
  DropdownMenuLabel,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import { getAllDoctors } from "@/services/profile";
import { resolveImageUrl } from "@/utils/media";

export const PatientDoctorsListPage = () => {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedSpecialties, setSelectedSpecialties] = useState<string[]>([]);

  const { data: doctors, isLoading } = useQuery({
    queryKey: ["all-doctors"],
    queryFn: () => getAllDoctors(),
  });

  const allSpecialties = useMemo(() => {
    const specs = doctors?.content
      ?.map((d) => d.specialization)
      .filter(Boolean) as string[];
    return [...new Set(specs)].sort();
  }, [doctors]);

  const filteredDoctors = useMemo(() => {
    return doctors?.content?.filter((doc) => {
      const matchesSearch =
        doc.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        doc.specialization?.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesSpecialty =
        selectedSpecialties.length === 0 ||
        selectedSpecialties.includes(doc.specialization ?? "");
      return matchesSearch && matchesSpecialty;
    });
  }, [doctors, searchTerm, selectedSpecialties]);

  const toggleSpecialty = (spec: string) => {
    setSelectedSpecialties((prev) =>
      prev.includes(spec) ? prev.filter((s) => s !== spec) : [...prev, spec],
    );
  };

  const clearFilters = () => {
    setSearchTerm("");
    setSelectedSpecialties([]);
  };

  const hasActiveFilters = searchTerm || selectedSpecialties.length > 0;

  return (
    <div className="container mx-auto py-8 space-y-8">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-primary/10 rounded-xl">
            <Stethoscope className="h-6 w-6 text-primary" />
          </div>
          <div>
            <h1 className="text-3xl font-bold">Encontrar Médicos</h1>
            <p className="text-muted-foreground">
              Conheça nossa equipe de especialistas e agende sua consulta.
            </p>
          </div>
        </div>

        {!isLoading && (
          <div className="flex items-center gap-2 text-sm text-muted-foreground bg-muted px-3 py-1.5 rounded-full">
            <Users className="h-4 w-4" />
            <span>
              {filteredDoctors?.length ?? 0} médico
              {(filteredDoctors?.length ?? 0) !== 1 ? "s" : ""} disponíve
              {(filteredDoctors?.length ?? 0) !== 1 ? "is" : "l"}
            </span>
          </div>
        )}
      </div>

      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Buscar por nome ou especialidade..."
            className="pl-9"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" className="gap-2 shrink-0">
              <SlidersHorizontal className="h-4 w-4" />
              Especialidade
              {selectedSpecialties.length > 0 && (
                <Badge className="ml-1 h-5 px-1.5 text-xs">
                  {selectedSpecialties.length}
                </Badge>
              )}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuLabel>Filtrar por especialidade</DropdownMenuLabel>
            <DropdownMenuSeparator />
            {allSpecialties.map((spec) => (
              <DropdownMenuCheckboxItem
                key={spec}
                checked={selectedSpecialties.includes(spec)}
                onCheckedChange={() => toggleSpecialty(spec)}
              >
                {spec}
              </DropdownMenuCheckboxItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>

        {hasActiveFilters && (
          <Button
            variant="ghost"
            size="icon"
            onClick={clearFilters}
            className="shrink-0 text-muted-foreground hover:text-foreground"
            title="Limpar filtros"
          >
            <X className="h-4 w-4" />
          </Button>
        )}
      </div>

      {selectedSpecialties.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {selectedSpecialties.map((spec) => (
            <Badge
              key={spec}
              variant="secondary"
              className="cursor-pointer gap-1 pr-1.5"
              onClick={() => toggleSpecialty(spec)}
            >
              {spec}
              <X className="h-3 w-3" />
            </Badge>
          ))}
        </div>
      )}

      {/* Grid */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-[220px] w-full rounded-xl" />
          ))}
        </div>
      ) : filteredDoctors?.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center space-y-3">
          <div className="p-4 bg-muted rounded-full">
            <Search className="h-8 w-8 text-muted-foreground" />
          </div>
          <p className="text-lg font-medium">Nenhum médico encontrado</p>
          <p className="text-sm text-muted-foreground max-w-xs">
            Tente ajustar os termos de busca ou os filtros de especialidade.
          </p>
          <Button variant="outline" size="sm" onClick={clearFilters}>
            Limpar filtros
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredDoctors?.map((doctor) => (
            <Card
              key={doctor.id}
              className="flex flex-col hover:shadow-lg transition-all duration-200 hover:-translate-y-0.5 group"
            >
              <CardContent className="flex-1 pt-6 pb-4">
                <div className="flex items-start gap-4">
                  <Avatar className="h-16 w-16 shrink-0 border-2 border-primary/10 group-hover:border-primary/30 transition-colors">
                    <AvatarImage
                      src={resolveImageUrl(doctor.profilePictureUrl)}
                      className="object-cover"
                      alt="Foto de perfil"
                    />
                    <AvatarFallback className="text-xl font-semibold">
                      {doctor.name?.charAt(0) || "D"}
                    </AvatarFallback>
                  </Avatar>

                  <div className="flex-1 min-w-0 space-y-1">
                    <h3 className="font-semibold text-base leading-tight truncate">
                      {doctor.name}
                    </h3>
                    <Badge variant="secondary" className="text-xs">
                      {doctor.specialization || "Clínico Geral"}
                    </Badge>
                    <p className="text-xs text-muted-foreground flex items-center gap-1 pt-0.5">
                      <Stethoscope className="h-3 w-3 shrink-0" />
                      <span className="truncate">
                        {doctor.department || "Departamento Médico"}
                      </span>
                    </p>
                  </div>
                </div>

                <p className="mt-4 text-sm text-muted-foreground line-clamp-2 leading-relaxed">
                  {doctor.biography ||
                    "Especialista dedicado com vasta experiência clínica e comprometimento com o bem-estar dos pacientes."}
                </p>
              </CardContent>

              <CardFooter className="pt-0 pb-4">
                <Button asChild className="w-full" variant="outline">
                  <Link to={`/patient/doctors/${doctor.id}`}>
                    Ver Perfil Completo
                  </Link>
                </Button>
              </CardFooter>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};
