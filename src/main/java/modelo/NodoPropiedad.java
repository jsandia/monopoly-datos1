package modelo;

/**
 * Clase NodoPropiedad
 * Representa un nodo simple para la lista enlazada de propiedades.
 * Guarda la propiedad y el enlace al siguiente nodo.
 */
public class NodoPropiedad {

    private Propiedad propiedad;
    private NodoPropiedad siguiente;

    // Constructor que inicializa el nodo con la propiedad dada
    public NodoPropiedad(Propiedad propiedad) {
        this.propiedad = propiedad;
        this.siguiente = null;
    }

    // Retorna la propiedad guardada
    public Propiedad getPropiedad() {
        return propiedad;
    }

    // Cambia la propiedad del nodo
    public void setPropiedad(Propiedad propiedad) {
        this.propiedad = propiedad;
    }

    // Retorna el puntero al siguiente nodo
    public NodoPropiedad getSiguiente() {
        return siguiente;
    }

    // Define el siguiente nodo en la lista
    public void setSiguiente(NodoPropiedad siguiente) {
        this.siguiente = siguiente;
    }
}