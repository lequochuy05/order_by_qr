package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.model.DiningTable;
import com.sacmauquan.qrordering.repository.DiningTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class DiningTableService {

    @Autowired
    private DiningTableRepository diningTableRepository;

    public Optional<DiningTable> getTableById(Long id) {
        return diningTableRepository.findById(id);
    }

    public List<DiningTable> getAllTablesSorted() {
        List<DiningTable> tables = diningTableRepository.findAll();
        tables.sort(Comparator.comparingInt(t -> Integer.parseInt(t.getTableNumber())));
        return tables;
    }

    public DiningTable createTable(DiningTable table){
        return diningTableRepository.save(table);
    }

    public DiningTable updateStatusAndCapacity(Long id, String status, Integer capacity) {
        DiningTable table = diningTableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bàn không tồn tại với id " + id));
        
        table.setStatus(status);
        table.setCapacity(capacity);
        
        return diningTableRepository.save(table);
    }

    public void deleteTable(Long id) {
        if (!diningTableRepository.existsById(id)) {
            throw new IllegalArgumentException("Bàn với ID " + id + " không tồn tại");
        }
        diningTableRepository.deleteById(id);
    }
}
