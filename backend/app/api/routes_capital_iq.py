"""Capital IQ Excel Bridge endpoints for template generation and upload parsing.
"""

from typing import List, Optional
from pydantic import BaseModel
from fastapi import APIRouter, HTTPException, UploadFile, File, Response
from fastapi.responses import StreamingResponse
from ..engine.capital_iq import create_capital_iq_bridge_workbook, parse_capital_iq_workbook

router = APIRouter()


class TemplateRequest(BaseModel):
    tickers: List[str] = [
        "IQ247543", "IQ28472", "IQ385732", "IQ94821", "IQ582910"
    ]
    calibration_date: str = "2025-02-26"
    valuation_date: str = "2026-06-30"
    currency: str = "EUR"
    frequency: str = "Weekly"
    lookback_years: int = 2


@router.post("/capital-iq/workbook")
def download_capital_iq_workbook(req: TemplateRequest):
    """Generates an Excel bridge template pre-populated with S&P Capital IQ plugin formulas."""
    try:
        wb_stream = create_capital_iq_bridge_workbook(
            tickers=req.tickers,
            calibration_date=req.calibration_date,
            valuation_date=req.valuation_date,
            currency=req.currency,
            frequency=req.frequency,
            lookback_years=req.lookback_years
        )
        wb_stream.seek(0)
        return StreamingResponse(
            wb_stream,
            media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            headers={"Content-Disposition": 'attachment; filename="Capital_IQ_Volatility_Bridge.xlsx"'}
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to generate Capital IQ bridge workbook: {str(e)}")


@router.post("/capital-iq/upload")
async def upload_capital_iq_workbook(file: UploadFile = File(...)):
    """Accepts a user-refreshed Capital IQ workbook, parses the data, and returns delevered volatility analyses."""
    if not file.filename.lower().endswith((".xlsx", ".xlsm")):
        raise HTTPException(status_code=400, detail="Only .xlsx or .xlsm files are supported.")
    
    try:
        content = await file.read()
        parsed = parse_capital_iq_workbook(content)
        return parsed
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to process Capital IQ file: {str(e)}")

