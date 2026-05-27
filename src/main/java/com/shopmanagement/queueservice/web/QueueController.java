package com.shopmanagement.queueservice.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.queueservice.model.QueueToken;
import com.shopmanagement.queueservice.service.QueueService;

@RestController
@RequestMapping("/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/today")
    public List<QueueToken> today(
            @RequestParam Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return queueService.todayQueue(doctorId, date);
    }

    @GetMapping("/display")
    public Map<String, Object> display(
            @RequestParam Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return queueService.displayBoard(branchId, date);
    }

    @PostMapping("/tokens")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueToken generateToken(@RequestBody QueueToken token) {
        return queueService.generateToken(token);
    }

    @PutMapping("/call-next")
    public QueueToken callNext(@RequestParam Long doctorId) {
        return queueService.callNext(doctorId);
    }

    @PutMapping("/tokens/{id}/start")
    public QueueToken start(@PathVariable Long id) {
        return queueService.startConsultation(id);
    }

    @PutMapping("/tokens/{id}/complete")
    public QueueToken complete(@PathVariable Long id) {
        return queueService.complete(id);
    }

    @PutMapping("/tokens/{id}/skip")
    public QueueToken skip(@PathVariable Long id) {
        return queueService.skip(id);
    }
}
