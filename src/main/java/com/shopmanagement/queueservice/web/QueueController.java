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

    /** Booked preferred slots for a doctor/date. Used by Reception slot grid (Available / Booked). */
    @GetMapping("/slots")
    public Map<String, Object> slots(
            @RequestParam Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return queueService.bookedSlots(doctorId, date);
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

    @GetMapping("/tokens/{id}")
    public QueueToken getToken(
            @PathVariable Long id,
            @RequestParam(required = false) Long doctorId) {
        return queueService.getToken(id, doctorId);
    }

    @PutMapping("/tokens/{id}/start")
    public QueueToken start(
            @PathVariable Long id,
            @RequestParam(required = false) Long doctorId) {
        return queueService.startConsultation(id, doctorId);
    }

    @PutMapping("/tokens/{id}/await-lab")
    public QueueToken awaitLab(
            @PathVariable Long id,
            @RequestParam(required = false) Long consultationId) {
        return queueService.awaitLabResults(id, consultationId);
    }

    @PutMapping("/tokens/lab-results-available")
    public org.springframework.http.ResponseEntity<QueueToken> markLabResultsAvailable(
            @RequestParam Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long consultationId) {
        QueueToken updated = queueService.markLabResultsAvailable(patientId, doctorId, consultationId);
        if (updated == null) {
            return org.springframework.http.ResponseEntity.noContent().build();
        }
        return org.springframework.http.ResponseEntity.ok(updated);
    }

    @GetMapping("/attention")
    public List<QueueToken> attention(
            @RequestParam Long doctorId,
            @RequestParam(required = false) Integer lookbackDays) {
        return queueService.attentionQueue(doctorId, lookbackDays);
    }

    @GetMapping("/open-for-patient")
    public List<QueueToken> openForPatient(
            @RequestParam Long patientId,
            @RequestParam(required = false) Long doctorId) {
        return queueService.openForPatient(patientId, doctorId);
    }

    @PutMapping("/tokens/{id}/complete")
    public QueueToken complete(@PathVariable Long id) {
        return queueService.complete(id);
    }

    @PutMapping("/tokens/{id}/cancel")
    public QueueToken cancel(@PathVariable Long id) {
        return queueService.cancel(id);
    }

    @PutMapping("/tokens/{id}/skip")
    public QueueToken skip(@PathVariable Long id) {
        return queueService.skip(id);
    }
}
