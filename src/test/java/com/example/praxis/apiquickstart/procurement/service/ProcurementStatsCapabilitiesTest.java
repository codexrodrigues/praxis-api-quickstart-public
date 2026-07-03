package com.example.praxis.apiquickstart.procurement.service;

import com.example.praxis.apiquickstart.config.DomainRuleBackendValidationPolicyResolver;
import com.example.praxis.apiquickstart.config.DomainRuleOptionSourcePolicyResolver;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementCompanyMapper;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementContractMapper;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementProductMapper;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementPurchaseOrderMapper;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementSupplierMapper;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementCompanyRepository;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementContractRepository;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementProductRepository;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementPurchaseOrderRepository;
import com.example.praxis.apiquickstart.procurement.repository.ProcurementSupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ProcurementStatsCapabilitiesTest {

    @Mock
    private ProcurementCompanyRepository companyRepository;
    @Mock
    private ProcurementCompanyMapper companyMapper;
    @Mock
    private ProcurementContractRepository contractRepository;
    @Mock
    private ProcurementContractMapper contractMapper;
    @Mock
    private ProcurementProductRepository productRepository;
    @Mock
    private ProcurementProductMapper productMapper;
    @Mock
    private ProcurementPurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private ProcurementPurchaseOrderMapper purchaseOrderMapper;
    @Mock
    private ProcurementSupplierRepository supplierRepository;
    @Mock
    private ProcurementSupplierMapper supplierMapper;
    @Mock
    private DomainRuleBackendValidationPolicyResolver backendValidationPolicyResolver;
    @Mock
    private DomainRuleOptionSourcePolicyResolver optionSourcePolicyResolver;
    @Mock
    private DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;

    @Test
    void shouldExposeStatsCapabilitiesForProcurementMasterData() {
        ProcurementCompanyService companies = new ProcurementCompanyService(companyRepository, companyMapper);
        ProcurementSupplierService suppliers = new ProcurementSupplierService(
                supplierRepository,
                supplierMapper,
                optionSourcePolicyResolver,
                workflowActionPolicyResolver
        );
        ProcurementProductService products = new ProcurementProductService(productRepository, productMapper);

        assertEquals(StatsSupportMode.AUTO, companies.getGroupByStatsSupportMode());
        assertTrue(companies.getStatsFieldRegistry().resolve("status").orElseThrow().groupByEligible());
        assertTrue(companies.getStatsFieldRegistry().resolve("state").orElseThrow().distributionTermsEligible());

        assertEquals(StatsSupportMode.AUTO, suppliers.getGroupByStatsSupportMode());
        assertTrue(suppliers.getStatsFieldRegistry().resolve("homologationStatus").orElseThrow().groupByEligible());
        assertTrue(suppliers.getStatsFieldRegistry().resolve("riskLevel").orElseThrow().distributionTermsEligible());

        assertEquals(StatsSupportMode.AUTO, products.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, products.getDistributionStatsSupportMode());
        assertTrue(products.getStatsFieldRegistry().resolve("categoryName").orElseThrow().groupByEligible());
        assertTrue(products.getStatsFieldRegistry().resolve("stockAvailable").orElseThrow().distributionHistogramEligible());
        assertTrue(products.getStatsFieldRegistry().resolve("stockAvailable").orElseThrow().supports(StatsMetric.SUM));
    }

    @Test
    void shouldExposeStatsCapabilitiesForProcurementCommercialFlow() {
        ProcurementContractService contracts = new ProcurementContractService(
                contractRepository,
                contractMapper,
                workflowActionPolicyResolver
        );
        ProcurementPurchaseOrderService purchaseOrders = new ProcurementPurchaseOrderService(
                purchaseOrderRepository,
                purchaseOrderMapper,
                supplierRepository,
                backendValidationPolicyResolver,
                workflowActionPolicyResolver
        );

        assertEquals(StatsSupportMode.AUTO, contracts.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, contracts.getTimeSeriesStatsSupportMode());
        assertTrue(contracts.getStatsFieldRegistry().resolve("supplierId").orElseThrow().groupByEligible());
        assertTrue(contracts.getStatsFieldRegistry().resolve("currency").orElseThrow().distributionTermsEligible());
        assertTrue(contracts.getStatsFieldRegistry().resolve("validUntil").orElseThrow().timeSeriesEligible());

        assertEquals(StatsSupportMode.AUTO, purchaseOrders.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, purchaseOrders.getTimeSeriesStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, purchaseOrders.getDistributionStatsSupportMode());
        assertTrue(purchaseOrders.getStatsFieldRegistry().resolve("status").orElseThrow().groupByEligible());
        assertTrue(purchaseOrders.getStatsFieldRegistry().resolve("orderDate").orElseThrow().timeSeriesEligible());
        assertTrue(purchaseOrders.getStatsFieldRegistry().resolve("quantity").orElseThrow().metricFieldEligible());
        assertTrue(purchaseOrders.getStatsFieldRegistry().resolve("quantity").orElseThrow().supports(StatsMetric.AVG));
    }
}
