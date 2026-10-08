import * as React from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export type SignInCardProps = {
  title: string;
  roleSubtitle: string;
  description: string;
  href: string;
  buttonText: string;
  icon?: React.ReactNode;
  features?: string[];
  isPopular?: boolean;
  hoverbg?: String;
};

function checkGreenIcon() {
  return (
    <svg
      className="w-4 h-4 text-green shrink-0"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M5 13l4 4L19 7"
      />
    </svg>
  );
}

function SignInCard({
  title,
  roleSubtitle,
  description,
  href,
  buttonText,
  icon,
  features,
  isPopular = false,
  hoverbg = "",
}: SignInCardProps) {
  return (
    <Card
      className={`w-full max-w-[400px] lg:max-w-[600px] py-4 flex flex-col justify-between transition-all duration-300  rounded-xl bg-white shadow-1 p-4 sm:p-7.5 xl:p-11`}
    >
      <div>
        <CardHeader className="pb-4">
          <div className="flex items-center gap-3.5 mb-2">
            {icon && (
              <div className="w-12 h-12 rounded-xl bg-blue/10 text-blue flex items-center justify-center text-xl shrink-0">
                {icon}
              </div>
            )}
            <div>
              <span className="text-xs font-semibold uppercase tracking-wider text-blue block">
                {roleSubtitle}
              </span>
              <CardTitle className="text-xl font-bold text-dark">
                {title}
              </CardTitle>
            </div>
          </div>
          <CardDescription className="text-body text-sm mt-1">
            {description}
          </CardDescription>
        </CardHeader>

        {features && features.length > 0 && (
          <CardContent className="pt-0 pb-4">
            <ul className="space-y-2.5">
              {features.map((feature, idx) => (
                <li
                  key={idx}
                  className="flex items-center text-sm text-dark-3 gap-2.5"
                >
                  {checkGreenIcon()}
                  <span>{feature}</span>
                </li>
              ))}
            </ul>
          </CardContent>
        )}
      </div>

      <CardFooter className="pt-2">
        <Link href={href} className="w-full">
          <Button
            variant={isPopular ? "default" : "outline"}
            className={`w-full h-11 text-base font-medium rounded-lg ${hoverbg}`}
          >
            {buttonText}
          </Button>
        </Link>
      </CardFooter>
    </Card>
  );
}

export default SignInCard;
