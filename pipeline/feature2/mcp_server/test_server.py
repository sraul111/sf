import json
import re
import sys
import unittest
from contextlib import redirect_stdout
from datetime import datetime, timedelta
from io import StringIO
from pathlib import Path
from unittest.mock import Mock, call, patch

sys.path.insert(0, str(Path(__file__).parent))
import server


class McpServerFeature2Tests(unittest.TestCase):
    def setUp(self):
        server.IDEMPOTENCY_CACHE.clear()

    def invoke(self, request, backend_responses=((200, {"sku": "CMP-CLUTCH", "totalQuantity": 3}),)):
        output = StringIO()
        with patch.object(server, "http_request", side_effect=backend_responses) as backend, redirect_stdout(output):
            server.handle_request(request)
        return json.loads(output.getvalue()), backend

    def test_crosscut_stock_checkUsesMockedStockToolResponse(self):
        response, backend = self.invoke(
            {"tool": "get_part_stock", "args": {"sku": "CMP-CLUTCH"}},
            [(200, {"id": 1, "sku": "CMP-CLUTCH", "name": "AC Compressor Clutch", "totalQuantity": 3})],
        )

        self.assertEqual(
            {"ok": True, "result": {"sku": "CMP-CLUTCH", "total": 3, "inUseNow": 0, "availableNow": 3}},
            response,
        )
        backend.assert_called_once_with("GET", "/api/parts/CMP-CLUTCH")

    def test_crosscut_reservationMapsMockedConflictWithoutRetryingOrMutating(self):
        conflict = {
            "code": "conflict",
            "message": "Insufficient stock",
            "details": {"total": 3, "requested": 2, "peakReserved": 2},
        }
        response, backend = self.invoke(
            {"tool": "reserve_part_for_job", "args": {"jobId": 12, "sku": "CMP-CLUTCH", "quantity": 2}},
            [(409, conflict)],
        )

        self.assertEqual({"ok": False, "status": 409, **conflict}, response)
        backend.assert_called_once_with(
            "POST",
            "/api/parts/CMP-CLUTCH/reservations",
            body={"jobId": 12, "quantity": 2},
        )

    def test_ec1McpRejectsNonPositiveAndNonIntegerReservationQuantitiesBeforeBackendCall(self):
        invalid_values = [0, -1, True, 1.5, "1", None]
        for quantity in invalid_values:
            with self.subTest(quantity=quantity):
                response, backend = self.invoke({
                    "tool": "reserve_part_for_job",
                    "args": {"jobId": 12, "sku": "CMP-CLUTCH", "quantity": quantity},
                })
                self.assertEqual((False, 400, "invalid_input"),
                                 (response.get("ok"), response.get("status"), response.get("code")),
                                 f"quantity {quantity!r} must be rejected")
                backend.assert_not_called()

    def test_ec2StockLookupReturnsCanonicalSkuAndMapsUnknownSku(self):
        canonical, canonical_backend = self.invoke(
            {"tool": "get_part_stock", "args": {"sku": "cmp-clutch"}},
            [(200, {"sku": "CMP-CLUTCH", "totalQuantity": 3})],
        )
        self.assertEqual("CMP-CLUTCH", canonical["result"]["sku"])
        canonical_backend.assert_called_once_with("GET", "/api/parts/cmp-clutch")

        missing, missing_backend = self.invoke(
            {"tool": "get_part_stock", "args": {"sku": "UNKNOWN"}},
            [(404, {"code": "not_found", "message": "Part not found", "details": {"sku": "UNKNOWN"}})],
        )
        self.assertEqual((False, 404, "not_found"),
                         (missing["ok"], missing["status"], missing["code"]))
        missing_backend.assert_called_once()

    def test_ec3AvailabilityMapsMissingTechnicianFromMockedBackend(self):
        response, backend = self.invoke(
            {
                "tool": "get_technician_availability",
                "args": {
                    "technicianId": 9999,
                    "jobStart": "2026-10-01T09:00:00Z",
                    "jobEnd": "2026-10-01T10:00:00Z",
                },
            },
            [(404, {"message": "not found"})],
        )

        self.assertEqual((False, 404, "not_found"),
                         (response["ok"], response["status"], response["code"]))
        backend.assert_called_once_with("GET", "/api/technicians/9999")

    def test_crosscutAvailabilityUsesWindowMatchingMockedMemoryTravelBufferFact(self):
        memory_fact = Mock(content="The standard travel buffer per job is 45 minutes.")
        buffer_minutes = int(re.search(r"(\d+) minutes", memory_fact.content).group(1))
        requested_start = datetime.fromisoformat("2026-10-01T09:00:00+00:00")
        requested_end = datetime.fromisoformat("2026-10-01T10:00:00+00:00")
        response, backend = self.invoke(
            {
                "tool": "get_technician_availability",
                "args": {
                    "technicianId": 1,
                    "jobStart": "2026-10-01T09:00:00Z",
                    "jobEnd": "2026-10-01T10:00:00Z",
                },
            },
            [(200, {"id": 1}), (200, [])],
        )

        self.assertTrue(response["ok"])
        self.assertEqual(
            {
                "start": (requested_start - timedelta(minutes=buffer_minutes)).strftime("%Y-%m-%dT%H:%M:%SZ"),
                "end": (requested_end + timedelta(minutes=buffer_minutes)).strftime("%Y-%m-%dT%H:%M:%SZ"),
            },
            response["result"]["bufferedWindow"],
        )
        self.assertEqual(
            [call("GET", "/api/technicians/1"), call("GET", "/api/technicians/1/jobs")],
            backend.call_args_list,
        )
        self.assertEqual(45, buffer_minutes)

    def test_crosscutToolContractRejectsUnexpectedArguments(self):
        response, backend = self.invoke({
            "tool": "get_part_stock",
            "args": {"sku": "CMP-CLUTCH", "unexpected": "must be rejected"},
        })

        self.assertEqual((False, 400, "invalid_input"),
                 (response.get("ok"), response.get("status"), response.get("code")))
        backend.assert_not_called()


if __name__ == "__main__":
    unittest.main()