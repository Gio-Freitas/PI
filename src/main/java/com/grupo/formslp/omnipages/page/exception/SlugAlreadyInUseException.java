package com.grupo.formslp.omnipages.page.exception;

// slug já usado por outra página não excluída -> 409 (SlugDuplicado no contrato)
public class SlugAlreadyInUseException extends RuntimeException {

    public SlugAlreadyInUseException(String slug) {
        super(slug);
    }
}
