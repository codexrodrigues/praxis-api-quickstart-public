package com.example.praxis.apiquickstart.rulelab;

/** Fronteira host-owned para adquirir facts por escopo e instante, sem executar decisao ou efeito. */
interface ExtraordinaryBenefitFactProvider {
    ExtraordinaryBenefitFactSnapshot load(RuleFactLookup lookup);
}
