package de.hf.myfinance.valuation.persistence;

import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.persistence.entities.ValueCurveEntity;
import de.hf.myfinance.valuation.persistence.repositories.CashflowRepository;
import de.hf.myfinance.valuation.persistence.repositories.InstrumentRepository;
import de.hf.myfinance.valuation.persistence.repositories.ValueCurveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DataReaderImpl implements DataReader{
    private final InstrumentRepository instrumentRepository;
    private final InstrumentMapper instrumentMapper;
    private final CashflowRepository cashflowRepository;
    private final CashflowMapper cashflowMapper;
    private final ValueCurveRepository valueCurveRepository;
    private final ValueCurveMapper valueCurveMapper;

    @Autowired
    public DataReaderImpl(InstrumentRepository instrumentRepository, InstrumentMapper instrumentMapper, CashflowRepository cashflowRepository, CashflowMapper cashflowMapper, ValueCurveRepository valueCurveRepository, ValueCurveMapper valueCurveMapper) {
        this.instrumentRepository = instrumentRepository;
        this.instrumentMapper = instrumentMapper;
        this.cashflowRepository = cashflowRepository;
        this.cashflowMapper = cashflowMapper;
        this.valueCurveRepository = valueCurveRepository;
        this.valueCurveMapper = valueCurveMapper;
    }

    @Override
    public Mono<Instrument> findByBusinesskey(String businesskey) {
        return instrumentRepository.findByBusinesskey(businesskey).map(e-> instrumentMapper.entityToApi(e));
    }

    @Override
    public Flux<Instrument> findAll() {
        return instrumentRepository.findAll().map(e-> instrumentMapper.entityToApi(e));
    }

    @Override
    public Flux<Cashflow> findAllCashflow4Instrument(String businesskey) {
        return cashflowRepository.findByInstrumentBusinesskey(businesskey).map(e-> cashflowMapper.entityToApi(e));
    }

    @Override
    public Flux<Instrument> findByParentBusinesskey(String parentBusinesskey){
        return instrumentRepository.findByParentBusinesskey(parentBusinesskey).map(e-> instrumentMapper.entityToApi(e));
    }

    @Override
    public Mono<ValueCurve> findValueCurveByInstrumentBusinesskey(String businesskey){
        return valueCurveRepository.findByInstrumentBusinesskey(businesskey).map(e-> valueCurveMapper.entityToApi(e));
    }

    @Override
    public Flux<Instrument> findByParentBusinesskeyAndInstrumentType(String parentBusinesskey, InstrumentType instrumentType){
        return instrumentRepository.findByParentBusinesskeyAndInstrumentType(parentBusinesskey, instrumentType).map(e-> instrumentMapper.entityToApi(e));
    }
}
