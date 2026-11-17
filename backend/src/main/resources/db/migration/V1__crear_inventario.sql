CREATE TABLE productos (
    uuid UUID PRIMARY KEY,
    nombre TEXT NOT NULL CHECK (btrim(nombre) <> ''),
    categoria TEXT NOT NULL CHECK (btrim(categoria) <> ''),
    ubicacion TEXT NOT NULL CHECK (btrim(ubicacion) <> ''),
    stock INTEGER NOT NULL CHECK (stock >= 0),
    stock_minimo INTEGER NOT NULL CHECK (stock_minimo >= 0),
    archivado BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    solicitud_alta TEXT NOT NULL
);

CREATE TABLE ajustes_stock (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    producto_uuid UUID NOT NULL REFERENCES productos (uuid),
    operacion_id UUID NOT NULL,
    delta INTEGER NOT NULL CHECK (delta <> 0),
    stock_resultante INTEGER NOT NULL CHECK (stock_resultante >= 0),
    version_resultante BIGINT NOT NULL CHECK (version_resultante >= 0),
    UNIQUE (producto_uuid, operacion_id)
);
