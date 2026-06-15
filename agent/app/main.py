from fastapi import FastAPI

from app.api.routes import plugin_router, router

app = FastAPI(title="SmartMail Agent", version="0.1.0")
app.include_router(router)
app.include_router(plugin_router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "smartmail-agent"}
