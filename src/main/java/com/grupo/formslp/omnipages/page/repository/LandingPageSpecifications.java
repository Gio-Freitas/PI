package com.grupo.formslp.omnipages.page.repository;

import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.enums.LandingPageStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// filtros da lista de landing pages (tela 1); tenant e exclusão lógica são sempre aplicados
public final class LandingPageSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private LandingPageSpecifications() {
    }

    public static Specification<LandingPage> list(UUID tenantId, String name, LandingPageStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (name != null && !name.isBlank()) {
                String pattern = "%" + escapeLike(name.trim().toLowerCase()) + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // "%" e "_" digitados na busca são texto, não curinga
    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
