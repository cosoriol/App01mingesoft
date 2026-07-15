#!/usr/bin/env python3
"""Genera SQL para sembrar datos sinteticos de volumen (Epica 7, Evaluacion 3).

Uso: python3 generate_volume_data.py <cantidad> <package_id> <user_id> > seed.sql

Genera <cantidad> reservas CONFIRMED (y su pago correspondiente) con fechas
distribuidas en 2026, para que el reporte de ventas (rango 2026-01-01 a
2026-12-31) las incluya. discount_details va vacio ("[]", JSON valido) para
no romper el parseo de BookingResponse.

Todo referencia un unico package_id/user_id "K6 Volume Test" dedicado (ver
volume-test.sh), para poder identificar y borrar estos datos despues.
"""
import random
import sys
from datetime import datetime, timedelta

def main():
    count = int(sys.argv[1])
    package_id = int(sys.argv[2])
    user_id = int(sys.argv[3])

    start = datetime(2026, 1, 1)
    end = datetime(2026, 12, 30)
    span_seconds = int((end - start).total_seconds())

    booking_rows = []
    for _ in range(count):
        created = start + timedelta(seconds=random.randint(0, span_seconds))
        amount = round(random.uniform(50, 2000), 2)
        booking_rows.append(
            f"({package_id},{user_id},1,{amount},0.00,0.00,{amount},'[]','',"
            f"'CONFIRMED','{created:%Y-%m-%d %H:%M:%S}','{created:%Y-%m-%d %H:%M:%S}')"
        )

    print("SET autocommit=0;")
    print(
        "INSERT INTO bookings (package_id,user_id,passenger_count,base_amount,"
        "discount_percentage,discount_amount,total_amount,discount_details,"
        "discount_summary,status,created_at,updated_at) VALUES"
    )
    print(",\n".join(booking_rows) + ";")

    print(
        "INSERT INTO payments (booking_id,amount,payment_method,card_last_four,"
        "card_holder_name,payment_status,payment_date) "
        "SELECT id, total_amount, 'CREDIT_CARD', '0000', 'K6 Volume Test', "
        "'APPROVED', created_at FROM bookings "
        f"WHERE user_id={user_id} AND package_id={package_id} "
        "AND id NOT IN (SELECT booking_id FROM payments);"
    )
    print("COMMIT;")


if __name__ == "__main__":
    main()
