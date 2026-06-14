package com.zenenation.backend.service.impl;

import com.zenenation.backend.dto.response.CodChargeSlabResponse;
import com.zenenation.backend.dto.response.DeliveryChargeSlabResponse;
import com.zenenation.backend.dto.response.ShippingConfigResponse;
import com.zenenation.backend.entity.CodChargeSlab;
import com.zenenation.backend.entity.DeliveryChargeSlab;
import com.zenenation.backend.repository.CodChargeSlabRepository;
import com.zenenation.backend.repository.DeliveryChargeSlabRepository;
import com.zenenation.backend.service.ShippingConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages delivery charge slabs (weight-based) and COD charge slabs (amount-based).
 * Admin can replace all slabs at once — old slabs are deleted, new ones inserted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingConfigServiceImpl implements ShippingConfigService {

    private final DeliveryChargeSlabRepository deliverySlabRepo;
    private final CodChargeSlabRepository codSlabRepo;

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryChargeSlab> getDeliverySlabs() {
        return deliverySlabRepo.findAllByOrderByMinWeightGramsAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CodChargeSlab> getCodSlabs() {
        return codSlabRepo.findAllByOrderByMinOrderAmountAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingConfigResponse getShippingConfig() {
        return ShippingConfigResponse.builder()
                .deliverySlabs(getDeliverySlabs().stream().map(this::toDeliveryResponse).collect(Collectors.toList()))
                .codSlabs(getCodSlabs().stream().map(this::toCodResponse).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public ShippingConfigResponse updateDeliverySlabs(List<DeliveryChargeSlab> slabs) {
        deliverySlabRepo.deleteAll();
        deliverySlabRepo.flush();

        for (DeliveryChargeSlab slab : slabs) {
            slab.setId(null); // Ensure new records are created
            deliverySlabRepo.save(slab);
        }

        log.info("Delivery charge slabs updated: {} slabs", slabs.size());
        return getShippingConfig();
    }

    @Override
    @Transactional
    public ShippingConfigResponse updateCodSlabs(List<CodChargeSlab> slabs) {
        codSlabRepo.deleteAll();
        codSlabRepo.flush();

        for (CodChargeSlab slab : slabs) {
            slab.setId(null); // Ensure new records are created
            codSlabRepo.save(slab);
        }

        log.info("COD charge slabs updated: {} slabs", slabs.size());
        return getShippingConfig();
    }

    // ── Mappers ──

    private DeliveryChargeSlabResponse toDeliveryResponse(DeliveryChargeSlab slab) {
        return DeliveryChargeSlabResponse.builder()
                .id(slab.getId())
                .minWeightGrams(slab.getMinWeightGrams())
                .maxWeightGrams(slab.getMaxWeightGrams())
                .charge(slab.getCharge())
                .build();
    }

    private CodChargeSlabResponse toCodResponse(CodChargeSlab slab) {
        return CodChargeSlabResponse.builder()
                .id(slab.getId())
                .minOrderAmount(slab.getMinOrderAmount())
                .maxOrderAmount(slab.getMaxOrderAmount())
                .extraCharge(slab.getExtraCharge())
                .build();
    }
}
