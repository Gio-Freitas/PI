package com.grupo.formslp.omnipages.page.service;

import com.grupo.formslp.omnipages.page.dto.BlockRequest;
import com.grupo.formslp.omnipages.page.dto.BlockResponse;
import com.grupo.formslp.omnipages.page.dto.LandingPageRequest;
import com.grupo.formslp.omnipages.page.enums.BlockType;
import com.grupo.formslp.omnipages.page.repository.LandingPageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// o que só o banco mostra: id gerado no flush, orphanRemoval, constraint de posição e a trava da página.
// Roda contra o Postgres configurado, como o OmnipagesApplicationTests; sem @Transactional de propósito,
// porque a constraint de posição só é checada no commit
@SpringBootTest
class LandingPageBlockIntegrationTest {

    @Autowired
    private LandingPageService pageService;
    @Autowired
    private LandingPageBlockService blockService;
    @Autowired
    private LandingPageRepository pageRepository;

    private final UUID tenantId = UUID.randomUUID();
    private UUID pageId;

    @BeforeEach
    void createPage() {
        String slug = "teste-blocos-" + UUID.randomUUID();
        pageId = pageService.create(tenantId, new LandingPageRequest("Teste", slug, null, null, null)).id();
    }

    @AfterEach
    void removePage() {
        pageRepository.deleteById(pageId);
    }

    @Test
    void fullEditingFlowKeepsPositionsContiguous() {
        BlockResponse hero = add(BlockType.HERO);
        BlockResponse text = add(BlockType.TEXT);
        BlockResponse footer = add(BlockType.FOOTER);
        assertNotNull(hero.id());
        assertEquals(2, footer.ordem());

        BlockResponse copy = blockService.duplicate(tenantId, pageId, hero.id());
        assertEquals(1, copy.ordem());

        blockService.reorder(tenantId, pageId, List.of(footer.id(), text.id(), copy.id(), hero.id()));
        blockService.delete(tenantId, pageId, text.id());

        List<BlockResponse> blocks = blockService.list(tenantId, pageId);
        assertEquals(List.of(footer.id(), copy.id(), hero.id()), blocks.stream().map(BlockResponse::id).toList());
        assertEquals(List.of(0, 1, 2), blocks.stream().map(BlockResponse::ordem).toList());
        assertTrue(pageService.get(tenantId, pageId).temAlteracaoNaoPublicada());
    }

    @Test
    void concurrentAddsWaitForEachOtherAndNeverShareAPosition() throws Exception {
        int editors = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(editors);
        try {
            List<Future<BlockResponse>> results = new ArrayList<>();
            for (int i = 0; i < editors; i++) {
                Callable<BlockResponse> addBlock = () -> {
                    start.await();
                    return add(BlockType.TEXT);
                };
                results.add(pool.submit(addBlock));
            }
            start.countDown();
            for (Future<BlockResponse> result : results) {
                result.get();
            }
        } finally {
            pool.shutdown();
        }

        List<Integer> positions = blockService.list(tenantId, pageId).stream().map(BlockResponse::ordem).toList();
        assertEquals(IntStream.range(0, editors).boxed().toList(), positions);
    }

    private BlockResponse add(BlockType type) {
        return blockService.add(tenantId, pageId, new BlockRequest(type, Map.of("titulo", type.name()), null));
    }
}
