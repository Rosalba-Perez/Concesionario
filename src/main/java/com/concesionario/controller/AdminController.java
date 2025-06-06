package com.concesionario.controller;

import com.concesionario.model.Rol;
// import com.concesionario.model.Trabajadores;
import com.concesionario.model.Vehiculo;
// import com.concesionario.repository.TrabajadoresRepository;
import com.concesionario.service.*;
import com.concesionario.repository.CitaRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.time.LocalDateTime;
import java.util.*;

import com.concesionario.dto.UsuarioDTO;
import com.concesionario.model.Cita;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

@Controller
@RequestMapping("/admin")
public class AdminController {
    // private final TrabajadorService trabajadorService;

    // TrabajadorService trabajadorService, TrabajadoresRepository trabajadoresRepository,
    // this.trabajadorService = trabajadorService; this.trabajadoresRepository = trabajadoresRepository;
    @Autowired
    public AdminController(
                           VehiculoService vehiculoService,
                           CitaService citaService,
                           NotificacionService notificacionService,
                           PasswordEncoder passwordEncoder,
                           Cloudinary cloudinary) {
        
        this.vehiculoService = vehiculoService;
        this.citaService = citaService;
        this.notificacionService = notificacionService;
        this.passwordEncoder = passwordEncoder;
        this.cloudinary = cloudinary;
    }
    // @Autowired
    // private TrabajadoresRepository trabajadoresRepository;

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private VehiculoService vehiculoService;
    @Autowired
    private CitaService citaService;
    @Autowired
    private NotificacionService notificacionService;
    @Autowired
    private Cloudinary cloudinary;

    // @Value("${upload.dir}")
    // private String uploadDir;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/Dashboard")
    public String dashboard(Model model) {
        // Estadísticas
        long totalCitas = citaService.contarTodasLasCitas();
        long totalUsuarios = usuarioService.contarUsuarios();
        long totalVehiculos = vehiculoService.contarTodosVehiculos();

        //paginacion de las citas
        // int page = 0;
        // int size = 5;
        // Page<Cita> paginaCitas = citaService.obtenerCitasPaginadas(page, size);

        // Listados
        List<Vehiculo> vehiculos = vehiculoService.obtenerVehiculosNormales();
        List<Vehiculo> anuncios = vehiculoService.obtenerDestacados(); 
        List<Cita> citasPendientes = citaService.obtenerCitasPendientes();

        // Agregar atributos al modelo
        // model.addAttribute("citas", paginaCitas.getContent());
        // model.addAttribute("currentPage", page);
        // model.addAttribute("totalPages", paginaCitas.getTotalPages());

        model.addAttribute("totalCitas", totalCitas);
        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("totalVehiculos", totalVehiculos);
        model.addAttribute("vehiculos", vehiculos);
        model.addAttribute("anuncios", anuncios);
        model.addAttribute("citas", citasPendientes);
        model.addAttribute("numeroNotificaciones", notificacionService.contarCitasNoLeidas());
        model.addAttribute("citasNoLeidas", notificacionService.obtenerCitasNoLeidas());

        return "Admin/Dashboard";
    }

    // ejemplo 2
    private String extraerPublicIdDesdeUrl(String url) {
    if (url == null || !url.contains("/")) return null;

    // Ejemplo de URL: https://res.cloudinary.com/demo/image/upload/v1234567890/carpeta/miimagen.jpg
    try {
        String[] partes = url.split("/");
        String nombreConExtension = partes[partes.length - 1];
        return url.substring(url.indexOf("/upload/") + 8, url.lastIndexOf(".")); // desde /upload/ hasta antes de .jpg
    } catch (Exception e) {
        return null;
    }
}


    // @GetMapping("/citas")
    // public String listarCitas(@RequestParam(defaultValue = "0") int page,
    //                           @RequestParam(defaultValue = "5") int size,
    //                           Model model) {
        
    //     Page<Cita> paginaCitas = citaService.obtenerCitasPaginadas(page, size);

    //     model.addAttribute("citas", paginaCitas.getContent());
    //     model.addAttribute("currentPage", page);
    //     model.addAttribute("totalPages", paginaCitas.getTotalPages());


    //     model.addAttribute("numeroNotificaciones", notificacionService.contarCitasNoLeidas());
    //     return "Admin/Citaslista";
    // }

    @PostMapping("/RegistroT")
    public String RegistroTr(
            @RequestParam("NombreUser") String nombre,
            @RequestParam("ApellidoUser") String apellido,
            @RequestParam("CorreoUser") String correo,
            @RequestParam("Identification") String identificacion,
            @RequestParam("password") String contrasena,
            Model model) {
        
        // if (trabajadorService.existeCorreoEnCualquierTabla(correo))
        if (usuarioService.existeCorreoEnCualquierTabla(correo)) {
            model.addAttribute("error", "Error: El correo ya está registrado (como usuario o trabajador)");
            model.addAttribute("NombreUser", nombre);
            model.addAttribute("ApellidoUser", apellido);
            model.addAttribute("CorreoUser", correo);
            model.addAttribute("Identification", identificacion);
            return "Admin/Dashboard";
        }
        
        // if (trabajadorService.existeIdentificacionEnCualquierTabla(identificacion))
        if (usuarioService.existeIdentificacionEnCualquierTabla(identificacion)) {
            model.addAttribute("error", "Error: La identificación ya está registrada (como usuario o trabajador)");
            model.addAttribute("NombreUser", nombre);
            model.addAttribute("ApellidoUser", apellido);
            model.addAttribute("CorreoUser", correo);
            model.addAttribute("Identification", identificacion);
            return "Admin/Dashboard";
        }

        try {
            usuarioService.registrarUsuario(
            nombre,
            apellido,
            correo,
            identificacion,
            contrasena,
            Rol.TRABAJADOR 
            
            );
            return "redirect:/admin/Dashboard?success=Trabajador+registrado+exitosamente";
        } catch (Exception e) {
        model.addAttribute("error", "Error inesperado al registrar trabajador");
        model.addAttribute("NombreUser", nombre);
        model.addAttribute("ApellidoUser", apellido);
        model.addAttribute("CorreoUser", correo);
        model.addAttribute("Identification", identificacion);
        return "Admin/Dashboard";
        }
    }

    @GetMapping("/notificaciones")
    @ResponseBody
    public List<Map<String, Object>> obtenerNotificaciones() {
        List<Cita> citas = notificacionService.obtenerCitasNoLeidas();

        return citas.stream().map(cita -> {
            Map<String, Object> citaMap = new HashMap<>();
            citaMap.put("id", cita.getId());

            UsuarioDTO usuario = cita.getUsuario();
            citaMap.put("nombres", usuario != null ? usuario.getNombre() : "");
            citaMap.put("apellidos", usuario != null ? usuario.getApellido() : "");
            // citaMap.put("nombres", cita.getNombres());
            // citaMap.put("apellidos", cita.getApellidos());

            citaMap.put("tipo", cita.getTipo());
            citaMap.put("fechaCreacion", cita.getFechaCreacion().toString());
            citaMap.put("comentario", cita.getComentario());
            return citaMap;
        }).collect(Collectors.toList());
    }

    @PostMapping("/marcar-leida/{id}")
    @ResponseBody
    public String marcarComoLeida(@PathVariable String id) {
        notificacionService.marcarComoLeida(id);
        return "OK";
    }

    @PostMapping("/marcar-todas-leidas")
    @ResponseBody
    public String marcarTodasComoLeidas() {
        notificacionService.marcarTodasComoLeidas();
        return "OK";
    }


    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("vehiculo", new Vehiculo());
        return "Admin/Nuevo";
    }


    @GetMapping("/obtener-vehiculo/{id}")
    @ResponseBody
    public Vehiculo obtenerVehiculoParaEdicion(@PathVariable String id) {
        Vehiculo vehiculo = vehiculoService.obtenerPorId(id);
        if (vehiculo.getColores() == null) {
            vehiculo.setColores(new ArrayList<>());
        }
        return vehiculo;
    }

    @PostMapping("/editar/{id}")
    public String editarVehiculo(
            @PathVariable String id,
            @ModelAttribute Vehiculo vehiculo,
            @RequestParam(value = "imagen", required = false) MultipartFile imagen,
            @RequestParam("motor") String motor,
            @RequestParam("transmision") String transmision,
            @RequestParam("combustible") String combustible,
            @RequestParam("pasajeros") Integer pasajeros,
            @RequestParam String colores, // Añade este parámetro
            @RequestParam("descripcion") String descripcion,
            Model model) {

        try {
            Vehiculo vehiculoExistente = vehiculoService.obtenerPorId(id);

            if (vehiculoExistente.getColores() == null) {
                vehiculoExistente.setColores(new ArrayList<>());
            }
            if (colores != null && !colores.isEmpty()) {
                List<String> listaColores = Arrays.stream(colores.split(","))
                        .map(String::trim)
                        .filter(color -> !color.isEmpty())
                        .collect(Collectors.toList());
                vehiculoExistente.setColores(listaColores);
            } else {
                vehiculoExistente.setColores(new ArrayList<>());
            }

            // if (vehiculoExistente == null) {
            //     model.addAttribute("error", "El vehículo no existe.");
            //     return "redirect:/admin/Dashboard";
            // }

            // Campos básicos
            vehiculoExistente.setMarca(vehiculo.getMarca());
            vehiculoExistente.setModelo(vehiculo.getModelo());
            vehiculoExistente.setAño(vehiculo.getAño());
            vehiculoExistente.setPrecio(vehiculo.getPrecio());
            vehiculoExistente.setCategoria(vehiculo.getCategoria());
            vehiculoExistente.setMotor(motor);
            vehiculoExistente.setTransmision(transmision);
            vehiculoExistente.setCombustible(combustible);
            vehiculoExistente.setPasajeros(pasajeros);
            vehiculoExistente.setDescripcion(descripcion);

            // Procesar colores
            if (colores != null && !colores.isEmpty()) {
                List<String> listaColores = Arrays.stream(colores.split(","))
                        .map(String::trim)
                        .filter(color -> !color.isEmpty())
                        .collect(Collectors.toList());
                vehiculoExistente.setColores(listaColores);
            } else {
                vehiculoExistente.setColores(new ArrayList<>());
            }

            // Manejo de imagen (el que ya tenías)
            if (imagen != null && !imagen.isEmpty()) {
                String rutaImagen = guardarImagen(imagen);
                vehiculoExistente.setImagenUrl(rutaImagen);
            }

            vehiculoService.guardarVehiculo(vehiculoExistente);
            return "redirect:/admin/Dashboard";

        } catch (IOException e) {
            model.addAttribute("error", "Error al guardar la imagen: " + e.getMessage());
            return "redirect:/admin/Dashboard";
        }
    }

// intento uno
    // @GetMapping("/eliminar/{id}")
    // public String eliminarVehiculo(@PathVariable String id) {
    //     Vehiculo vehiculo = vehiculoService.obtenerPorId(id);

    //     if (vehiculo.getImagenPublicId() != null && !vehiculo.getImagenPublicId().isEmpty()) {
    //         try {
    //             cloudinary.uploader().destroy(vehiculo.getImagenPublicId(), ObjectUtils.emptyMap());
    //         } catch (IOException e) {
    //             throw new RuntimeException("Error al eliminar la imagen de Cloudinary", e);
    //         }
    //     }

    //     vehiculoService.eliminarVehiculo(id);
    //     return "redirect:/admin/Dashboard";
    // }

    // private Map<String, String> guardarImagenCloudinary(MultipartFile imagen) throws IOException {
    //     Map uploadResult = cloudinary.uploader().upload(imagen.getBytes(), ObjectUtils.emptyMap());
    //     String url = (String) uploadResult.get("secure_url");
    //     String publicId = (String) uploadResult.get("public_id");

    //     return Map.of("url", url, "publicId", publicId);
    // }
    // intento 2
@GetMapping("/eliminar/{id}")
public String eliminarVehiculo(@PathVariable String id) {
    Vehiculo vehiculo = vehiculoService.obtenerPorId(id);

    if (vehiculo.getImagenUrl() != null) {
        try {
            // Extraer el public_id de Cloudinary desde la URL
            String url = vehiculo.getImagenUrl();
            String publicId = extraerPublicIdDesdeUrl(url);

            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar la imagen en Cloudinary", e);
        }
    }

    vehiculoService.eliminarVehiculo(id);
    return "redirect:/admin/Dashboard";
}
private String guardarImagen(MultipartFile imagen) throws IOException {
    if (imagen == null || imagen.isEmpty()) {
        throw new IllegalArgumentException("La imagen está vacía o es nula.");
    }

    Map uploadResult = cloudinary.uploader().upload(imagen.getBytes(), ObjectUtils.emptyMap());

    return (String) uploadResult.get("secure_url");
}

    
// de manera local
    // @GetMapping("/eliminar/{id}")
    // public String eliminarVehiculo(@PathVariable String id) {
    //     Vehiculo vehiculo = vehiculoService.obtenerPorId(id);
    //     if (vehiculo.getImagenUrl() != null) {
    //         Path rutaImagen = Paths.get(uploadDir + vehiculo.getImagenUrl().replace("/uploads/", ""));
    //         try {
    //             Files.deleteIfExists(rutaImagen);
    //         } catch (IOException e) {
    //             throw new RuntimeException("Error al eliminar la imagen", e);
    //         }
    //     }
    //     vehiculoService.eliminarVehiculo(id);
    //     return "redirect:/admin/Dashboard";
    // }

    // private String guardarImagen(MultipartFile imagen) throws IOException {
    //     Path uploadPath = Paths.get(uploadDir);
    //     if (!Files.exists(uploadPath)) {
    //         Files.createDirectories(uploadPath);
    //     }

    //     String nombreImagen = System.currentTimeMillis() + "_" + imagen.getOriginalFilename();
    //     Path rutaCompleta = uploadPath.resolve(nombreImagen);
    //     Files.copy(imagen.getInputStream(), rutaCompleta, StandardCopyOption.REPLACE_EXISTING);

    //     return "/uploads/" + nombreImagen;
    // }



    @GetMapping("/citas")
    public String listarCitas(Model model) {
        List<Cita> citas = citaService.obtenerTodasLasCitas();
        model.addAttribute("citas", citas);
        model.addAttribute("numeroNotificaciones", notificacionService.contarCitasNoLeidas());
        return "Admin/CitasLista";
    }

    @ResponseBody
    @PostMapping("/citas/{id}/cambiar-estado")
    public String cambiarEstadoCita(@PathVariable String id, @RequestParam String estado) {
        Cita cita = citaService.obtenerCitaPorId(id);
        if (cita == null) {
            return "Cita no encontrada";
        }

        cita.setEstado(estado);
        cita.setLeida(true);
        citaService.guardarCita(cita);

        return "OK";
    }
    @ResponseBody
    @PostMapping("/citas/{id}/asignar-fecha")
    public String asignarFechaCita(@PathVariable String id, @RequestParam String fecha) {
        Cita cita = citaService.obtenerCitaPorId(id);
        if (cita == null) {
            return "Cita no encontrada";
        }

        LocalDateTime fechaHora = LocalDateTime.parse(fecha.replace(" ", "T"));
        cita.setFechaAsignada(fechaHora);
        citaService.guardarCita(cita);

        return "OK";
    }
    @ResponseBody
    @GetMapping("/citas/{id}/notas")
    public String obtenerNotasCita(@PathVariable String id) {
        Cita cita = citaService.obtenerCitaPorId(id);
        return (cita != null && cita.getNotasAdmin() != null) ? cita.getNotasAdmin() : "";
    }
    @ResponseBody
    @PostMapping("/citas/{id}/guardar-notas")
    public String guardarNotasCita(@PathVariable String id, @RequestParam String notas) {
        Cita cita = citaService.obtenerCitaPorId(id);
        if (cita == null) {
            return "Cita no encontrada";
        }

        cita.setNotasAdmin(notas);
        citaService.guardarCita(cita);

        return "OK";
    }

    @ResponseBody
    @PostMapping("/citas/{id}/cancelar-fecha")
    public String cancelarFechaCita(@PathVariable String id) {
        Cita cita = citaService.obtenerCitaPorId(id);
        if (cita == null) {
            return "Cita no encontrada";
        }

        cita.setFechaAsignada(null);
        citaService.guardarCita(cita);

        return "OK";
    }

    @ResponseBody
    @PostMapping("/citas/{id}/volver-pendiente")
    public String volverAPendiente(@PathVariable String id) {
        Cita cita = citaService.obtenerCitaPorId(id);
        if (cita == null) {
            return "Cita no encontrada";
        }

        cita.setEstado("Pendiente");
        cita.setFechaAsignada(null);
        citaService.guardarCita(cita);

        return "OK";
    }

    @ResponseBody
    @GetMapping("/citas/{id}/datos")
    public Map<String, Object> obtenerDatosCita(@PathVariable String id) {
        Cita cita = citaService.obtenerCitaPorId(id);
        if (cita == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada");
        }

        Map<String, Object> datos = new HashMap<>();
        datos.put("estado", cita.getEstado());
        datos.put("fechaAsignada", cita.getFechaAsignada());
        datos.put("notasAdmin", cita.getNotasAdmin());

        return datos;
    }

    @GetMapping("/anuncios")
    public String listarAnuncios(Model model) {
        model.addAttribute("anuncios", vehiculoService.obtenerDestacados());
        return "Admin/Dashboard";
    }

    // Formulario de creación (en el mismo dashboard)
    @PostMapping("/guardar-anuncio")
    public String guardarAnuncio(
            @RequestParam String marca,
            @RequestParam String modelo,
            @RequestParam int año,
            @RequestParam double precio,
            @RequestParam String categoria,
            @RequestParam String motor,
            @RequestParam String transmision,
            @RequestParam String combustible,
            @RequestParam int pasajeros,
            @RequestParam String descripcion,
            @RequestParam MultipartFile imagen,
            Model model) throws IOException {

        try {
            Vehiculo anuncio = new Vehiculo();
            anuncio.setMarca(marca);
            anuncio.setModelo(modelo);
            anuncio.setAño(año);
            anuncio.setPrecio(precio);
            anuncio.setCategoria(categoria);
            anuncio.setMotor(motor);
            anuncio.setTransmision(transmision);
            anuncio.setCombustible(combustible);
            anuncio.setPasajeros(pasajeros);
            anuncio.setDescripcion(descripcion);
            anuncio.setDestacado(true); // Es un anuncio

            vehiculoService.crearAnuncio(anuncio, imagen);
            return "redirect:/admin/Dashboard";

        } catch (Exception e) {
            model.addAttribute("error", "Error al crear el anuncio: " + e.getMessage());
            return "redirect:/admin/Dashboard?error=Error+al+crear+anuncio";
        }
    }

    // Eliminar anuncio (y vehículo asociado)
    @PostMapping("/anuncios/eliminar/{id}")
    public String eliminarAnuncio(@PathVariable String id) {
        vehiculoService.eliminarVehiculo(id); // Elimina permanentemente
        return "redirect:/admin/anuncios";
    }
    @PostMapping("/guardar-vehiculo")
    public String guardarVehiculoNormal(
            @RequestParam String marca,
            @RequestParam String modelo,
            @RequestParam int año,
            @RequestParam double precio,
            @RequestParam String categoria,
            @RequestParam String motor,
            @RequestParam String transmision,
            @RequestParam String combustible,
            @RequestParam int pasajeros,
            @RequestParam String descripcion,
            @RequestParam String colores,  // Nuevo parámetro
            @RequestParam MultipartFile imagen) throws IOException {

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setMarca(marca);
        vehiculo.setModelo(modelo);
        vehiculo.setAño(año);
        vehiculo.setPrecio(precio);
        vehiculo.setCategoria(categoria);
        vehiculo.setMotor(motor);
        vehiculo.setTransmision(transmision);
        vehiculo.setCombustible(combustible);
        vehiculo.setPasajeros(pasajeros);
        vehiculo.setDescripcion(descripcion);
        vehiculo.setDestacado(false);

        // Procesar los colores
        if (colores != null && !colores.isEmpty()) {
            List<String> listaColores = Arrays.stream(colores.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
            vehiculo.setColores(listaColores);
        }

        vehiculoService.crearVehiculoNormal(vehiculo, imagen);
        return "redirect:/admin/Dashboard";
    }
}