"""Health check API route.
"""

from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
def health_check():
    return {
        "status": "ok",
        "service": "contingent-claims-valuation-engine",
        "version": "1.0.0"
    }

