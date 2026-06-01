package com.elis_lopez.terminalAutobusesBackend.exception;

/**
 * Excepción lanzada cuando no se encuentra un recurso por su ID.
 * <p>
 * Se utiliza en los servicios para indicar que una entidad solicitada
 * no existe en la base de datos, resultando en un error 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final Long resourceId;

    /**
     * Crea una excepción con el nombre del recurso y su ID.
     *
     * @param resourceName nombre descriptivo del recurso (ej. "Terminal", "Usuario")
     * @param resourceId   identificador del recurso no encontrado
     */
    public ResourceNotFoundException(String resourceName, Long resourceId) {
        super(resourceName + " no encontrado con id: " + resourceId);
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    /**
     * @return nombre del recurso no encontrado
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * @return ID del recurso no encontrado
     */
    public Long getResourceId() {
        return resourceId;
    }
}
