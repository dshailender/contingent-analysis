"""Export API endpoints for generating OpenPyXL workbooks and watermarked PDFs.
"""

from typing import Union
import io
from fastapi import APIRouter, HTTPException, Response
from fastapi.responses import StreamingResponse

from ..engine.models import ValuationRequest, ValuationResponse
from ..engine.pipeline import calculate_valuation
from ..engine.exports_excel import generate_valuation_workbook
from ..engine.exports_pdf import generate_valuation_pdf

router = APIRouter()


def _ensure_response(payload: Union[ValuationResponse, ValuationRequest]) -> ValuationResponse:
    if isinstance(payload, ValuationResponse):
        return payload
    return calculate_valuation(payload)


@router.post("/exports/excel")
def export_excel_report(payload: Union[ValuationResponse, ValuationRequest]):
    """Generates an executive multi-tab Excel (.xlsx) workbook for the valuation model."""
    try:
        res = _ensure_response(payload)
        wb_stream = generate_valuation_workbook(res)
        wb_stream.seek(0)
        filename = f"{res.company_name.replace(' ', '_')}_Valuation_Report.xlsx"
        return StreamingResponse(
            wb_stream,
            media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            headers={"Content-Disposition": f'attachment; filename="{filename}"'}
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Excel generation failed: {str(e)}")


@router.post("/exports/pdf")
async def export_pdf_report(payload: Union[ValuationResponse, ValuationRequest]):
    """Generates a landscape executive PDF with visible diagonal 'HIGHLY CONFIDENTIAL' watermark on every page."""
    try:
        res = _ensure_response(payload)
        pdf_bytes = await generate_valuation_pdf(res)
        filename = f"{res.company_name.replace(' ', '_')}_Valuation_Report.pdf"
        return Response(
            content=pdf_bytes,
            media_type="application/pdf",
            headers={"Content-Disposition": f'attachment; filename="{filename}"'}
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"PDF generation failed: {str(e)}")

