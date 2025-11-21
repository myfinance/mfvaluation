package de.hf.myfinance.valuation.persistence;

import de.hf.framework.exceptions.MFException;
import de.hf.myfinance.exception.MFMsgKey;
import de.hf.myfinance.restmodel.*;
import de.hf.myfinance.valuation.persistence.entities.PositionEntity;
import de.hf.myfinance.valuation.persistence.entities.PositionKey;
import de.hf.myfinance.valuation.persistence.entities.PositionValueEntity;
import de.hf.myfinance.valuation.persistence.entities.TradeEntity;
import de.hf.myfinance.valuation.persistence.mapper.CashflowMapper;
import de.hf.myfinance.valuation.persistence.mapper.EndOfDayPricesMapper;
import de.hf.myfinance.valuation.persistence.mapper.InstrumentMapper;
import de.hf.myfinance.valuation.persistence.mapper.TradeMapper;
import de.hf.myfinance.valuation.persistence.mapper.ValueCurveMapper;
import de.hf.myfinance.valuation.persistence.repositories.CashflowRepository;
import de.hf.myfinance.valuation.persistence.repositories.EndOfDayPricesRepository;
import de.hf.myfinance.valuation.persistence.repositories.InstrumentRepository;
import de.hf.myfinance.valuation.persistence.repositories.PositionRepository;
import de.hf.myfinance.valuation.persistence.repositories.PositionValueRepository;
import de.hf.myfinance.valuation.persistence.repositories.TradeRepository;
import de.hf.myfinance.valuation.persistence.repositories.ValueCurveRepository;

import java.util.List;
import java.util.TreeMap;
import java.time.LocalDate;

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
    private final EndOfDayPricesRepository endOfDayPricesRepository;
    private final EndOfDayPricesMapper endOfDayPricesMapper;
    private final PositionRepository positionRepository;
    private final PositionValueRepository positionValueRepository;
    private final TradeRepository tradeRepository;
    private final TradeMapper tradeMapper;

    public DataReaderImpl(InstrumentRepository instrumentRepository, InstrumentMapper instrumentMapper,
                          CashflowRepository cashflowRepository, CashflowMapper cashflowMapper,
                          ValueCurveRepository valueCurveRepository, ValueCurveMapper valueCurveMapper,
                          EndOfDayPricesRepository endOfDayPricesRepository, EndOfDayPricesMapper endOfDayPricesMapper, 
                          PositionRepository positionRepository, PositionValueRepository positionValueRepository,
                          TradeRepository tradeRepository, TradeMapper tradeMapper) {
        this.instrumentRepository = instrumentRepository;
        this.instrumentMapper = instrumentMapper;
        this.cashflowRepository = cashflowRepository;
        this.cashflowMapper = cashflowMapper;
        this.valueCurveRepository = valueCurveRepository;
        this.valueCurveMapper = valueCurveMapper;
        this.endOfDayPricesRepository = endOfDayPricesRepository;
        this.endOfDayPricesMapper = endOfDayPricesMapper;
        this.positionRepository = positionRepository;
        this.positionValueRepository = positionValueRepository;
        this.tradeRepository = tradeRepository;
        this.tradeMapper = tradeMapper;
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

    @Override
    public Mono<EndOfDayPrices> findPricesByInstrumentBusinesskey(String businesskey){
        return endOfDayPricesRepository.findByInstrumentBusinesskey(businesskey).map(e-> endOfDayPricesMapper.entityToApi(e));
    }

    @Override
    public Flux<ValueCurve> findValueCurvesByBusinesskeyIn(Iterable<String> businesskeyIterable){
        return valueCurveRepository.findByInstrumentBusinesskeyIn(businesskeyIterable).map(e-> valueCurveMapper.entityToApi(e));
    }

    @Override
    public Mono<ValueCurve> findPositonByKey(String depotBusinessKey, String securityBusinessKey) {
        var positionkey = new PositionKey(depotBusinessKey, securityBusinessKey);
        return positionRepository.findByPositionKey(positionkey).map(this::positionToValueCurve);
    }

    @Override
    public Flux<Trade> findTradesByKey(String depotBusinessKey, String securityBusinessKey) {
        return tradeRepository.findByDepotBusinessKeyAndSecurityBusinessKey(depotBusinessKey, securityBusinessKey).switchIfEmpty(handleNotExisting()).map(tradeMapper::entityToApi);
    }
    private Flux<TradeEntity> handleNotExisting(){
        return Flux.error(new MFException(MFMsgKey.UNKNOWN_INSTRUMENT_EXCEPTION, "No Trades for this Id available."));
    }

    private ValueCurve positionToValueCurve(PositionEntity position){
        var valueCurve = new ValueCurve(position.getPositionKey().getSecurityBusinessKey());
        valueCurve.setParentBusinesskey(position.getPositionKey().getDepotBusinessKey());
        valueCurve.setValueCurve(new TreeMap<LocalDate,Double>(position.getPositionCurve()));
        return valueCurve;
    }

    @Override
    public Flux<ValueCurve> findPositonValueByDepotKey(String depotBusinessKey){
        return positionValueRepository.findByDepotBusinessKeyAndValuationType(depotBusinessKey, ValuationType.MARKETVALUE).map(this::positionValueToValueCurve);
    }

    @Override
    public Flux<ValueCurve> findPositonBySecurityKey(String securityKey){
        return positionRepository.findByPositionKey_SecurityBusinessKey(securityKey).map(this::positionToValueCurve);
    }

    private ValueCurve positionValueToValueCurve(PositionValueEntity positionValue){
        var valueCurve = new ValueCurve(positionValue.getPositionValueKey().getSecurityBusinessKey());
        valueCurve.setParentBusinesskey(positionValue.getPositionValueKey().getDepotBusinessKey());
        valueCurve.setValueCurve(new TreeMap<LocalDate,Double>(positionValue.getPositionValueCurve()));
        return valueCurve;
    }

    @Override
    public Flux<Instrument> findByValueBudget(String valueBudget){
        return instrumentRepository.findByValueBudget(valueBudget).map(e-> instrumentMapper.entityToApi(e));
    }

    @Override
    public Flux<ValueCurve> findAllPostions(List<String> depots) {
        return positionRepository.findByPositionKey_DepotBusinessKeyIn(depots).map(this::positionToValueCurve);
    }

    @Override
    public Flux<ValueCurve> findAllPostionValues(List<String> depots) {
        return positionValueRepository.findByDepotBusinessKeyInAndValuationType(depots, ValuationType.MARKETVALUE).map(this::positionValueToValueCurve);
    }
}
